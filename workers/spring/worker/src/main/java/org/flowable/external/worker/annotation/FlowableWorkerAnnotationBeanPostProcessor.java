/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.external.worker.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import org.flowable.external.worker.config.FlowableWorkerConfigUtils;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerEndpointRegistrar;
import org.flowable.external.worker.config.FlowableWorkerEndpointRegistry;
import org.flowable.external.worker.config.MethodFlowableWorkerEndpoint;
import org.flowable.external.worker.config.FlowableWorkerEndpoint;

/**
 * Bean post-processor that registers methods annotated with {@link FlowableWorker} to be invoked by a Flowable worker container created under the cover
 * by a {@link FlowableWorkerContainerFactory} according to the attributes of the annotation.
 *
 * <p>Annotated methods can use flexible arguments as defined by {@link FlowableWorker}.
 *
 * <p>This post-processor is automatically registered by the {@link EnableFlowableWorker} annotation.
 *
 * <p>Autodetects any {@link FlowableWorkerConfigurer} instances in the container, allowing for customization of the registry to be used, the default container
 * factory or for fine-grained control over endpoints registration. See the {@link EnableFlowableWorker} javadocs for complete usage details.
 *
 * @author Filip Hrisafov
 * @see FlowableWorker
 * @see EnableFlowableWorker
 * @see FlowableWorkerConfigurer
 * @see FlowableWorkerEndpointRegistrar
 * @see FlowableWorkerEndpointRegistry
 * @see FlowableWorkerEndpoint
 * @see MethodFlowableWorkerEndpoint
 */
public class FlowableWorkerAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

    /**
     * The bean name of the default {@link FlowableWorkerContainerFactory}.
     */
    static final String DEFAULT_FLOWABLE_WORKER_CONTAINER_FACTORY_BEAN_NAME = "flowableWorkerContainerFactory";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String containerFactoryBeanName = DEFAULT_FLOWABLE_WORKER_CONTAINER_FACTORY_BEAN_NAME;

    protected FlowableWorkerEndpointRegistry endpointRegistry;

    protected final MessageHandlerMethodFactoryAdapter messageHandlerMethodFactory =
            new MessageHandlerMethodFactoryAdapter();

    protected BeanFactory beanFactory;

    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    protected BeanExpressionContext expressionContext;

    protected final FlowableWorkerEndpointRegistrar registrar = new FlowableWorkerEndpointRegistrar();

    protected final AtomicInteger counter = new AtomicInteger();

    protected final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Set the name of the {@link FlowableWorkerContainerFactory} to use by default.
     * <p>If none is specified, "flowableWorkerContainerFactory" is assumed to be defined.
     */
    public void setContainerFactoryBeanName(String containerFactoryBeanName) {
        this.containerFactoryBeanName = containerFactoryBeanName;
    }

    /**
     * Set the {@link FlowableWorkerEndpointRegistry} that will hold the created endpoint and manage the lifecycle of the related worker container.
     */
    public void setEndpointRegistry(FlowableWorkerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    /**
     * Making a {@link BeanFactory} available is optional; if not set,  {@link FlowableWorkerConfigurer} beans won't get autodetected and an
     * {@link #setEndpointRegistry endpoint registry} has to be explicitly configured.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.registrar.setBeanFactory(beanFactory);
        if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
            this.resolver = clbf.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Remove resolved singleton classes from cache
        this.nonAnnotatedClasses.clear();

        if (this.beanFactory instanceof ListableBeanFactory lbf) {
            // Apply FlowableWorkerConfigurer beans from the BeanFactory, if any
            Map<String, FlowableWorkerConfigurer> beans = lbf.getBeansOfType(FlowableWorkerConfigurer.class);
            List<FlowableWorkerConfigurer> configurers = new ArrayList<>(beans.values());
            AnnotationAwareOrderComparator.sort(configurers);
            for (FlowableWorkerConfigurer configurer : configurers) {
                configurer.configureFlowableWorkers(this.registrar);
            }
        }

        if (this.containerFactoryBeanName != null) {
            this.registrar.setContainerFactoryBeanName(this.containerFactoryBeanName);
        }

        if (this.registrar.getEndpointRegistry() == null) {
            // Determine FlowableWorkerEndpointRegistry bean from the BeanFactory
            if (this.endpointRegistry == null) {
                Assert.state(this.beanFactory != null, "BeanFactory must be set to find endpoint registry by bean name");
                this.endpointRegistry = this.beanFactory.getBean(
                        FlowableWorkerConfigUtils.FLOWABLE_WORKER_REGISTRY_BEAN_NAME, FlowableWorkerEndpointRegistry.class);
            }
            this.registrar.setEndpointRegistry(this.endpointRegistry);
        }

        // Set the custom handler method factory once resolved by the configurer
        MessageHandlerMethodFactory handlerMethodFactory = this.registrar.getMessageHandlerMethodFactory();
        if (handlerMethodFactory != null) {
            this.messageHandlerMethodFactory.setMessageHandlerMethodFactory(handlerMethodFactory);
        }

        // Actually register all workers
        this.registrar.afterPropertiesSet();
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AopInfrastructureBean || bean instanceof FlowableWorkerContainerFactory ||
                bean instanceof FlowableWorkerEndpointRegistry) {
            // Ignore AOP infrastructure such as scoped proxies.
            return bean;
        }

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        if (!this.nonAnnotatedClasses.contains(targetClass) &&
                AnnotationUtils.isCandidateClass(targetClass, FlowableWorker.class)) {
            Map<Method, Set<FlowableWorker>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<Set<FlowableWorker>>) method -> {
                        Set<FlowableWorker> workerMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                                method, FlowableWorker.class, FlowableWorkers.class);
                        return (!workerMethods.isEmpty() ? workerMethods : null);
                    });
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.add(targetClass);
                if (logger.isTraceEnabled()) {
                    logger.trace("No @FlowableWorker annotations found on bean type: " + targetClass);
                }
            } else {
                // Non-empty set of methods
                annotatedMethods.forEach((method, workers) ->
                        workers.forEach(worker -> processFlowableWorker(worker, method, bean)));
                if (logger.isDebugEnabled()) {
                    logger.debug(annotatedMethods.size() + " @FlowableWorker methods processed on bean '" + beanName +
                            "': " + annotatedMethods);
                }
            }
        }
        return bean;
    }

    /**
     * Process the given {@link FlowableWorker} annotation on the given method,
     * registering a corresponding endpoint for the given bean instance.
     *
     * @param flowableWorker the annotation to process
     * @param method the annotated method
     * @param bean the instance to invoke the method on
     * @see #createMethodFlowableWorkerEndpoint()
     * @see FlowableWorkerEndpointRegistrar#registerEndpoint
     */
    protected void processFlowableWorker(FlowableWorker flowableWorker, Method method, Object bean) {
        Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());

        MethodFlowableWorkerEndpoint endpoint = createMethodFlowableWorkerEndpoint();
        endpoint.setBean(bean);
        endpoint.setMethod(invocableMethod);
        endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory);
        endpoint.setId(getEndpointId(flowableWorker));
        endpoint.setTopic(resolve(flowableWorker.topic()));
        if (StringUtils.hasText(flowableWorker.lockDuration())) {
            endpoint.setLockDuration(resolveExpressionAsDuration(flowableWorker.lockDuration(), "lockDuration"));
        }
        if (StringUtils.hasText(flowableWorker.pollingInterval())) {
            endpoint.setPollingInterval(resolveExpressionAsDuration(flowableWorker.pollingInterval(), "pollingInterval"));
        }
        if (StringUtils.hasText(flowableWorker.numberOfRetries())) {
            endpoint.setNumberOfRetries(resolveExpressionAsInteger(flowableWorker.numberOfRetries(), "numberOfRetries"));
        }
        if (StringUtils.hasText(flowableWorker.numberOfTasks())) {
            endpoint.setNumberOfTasks(resolveExpressionAsInteger(flowableWorker.numberOfTasks(), "numberOfTasks"));
        }

        if (StringUtils.hasText(flowableWorker.concurrency())) {
            endpoint.setConcurrency(resolveExpressionAsInteger(flowableWorker.concurrency(), "concurrency"));
        }

        this.registrar.registerEndpoint(endpoint, null);
    }

    /**
     * Instantiate an empty {@link MethodFlowableWorkerEndpoint} for further
     * configuration with provided parameters in {@link #processFlowableWorker}.
     *
     * @return a new {@code MethodFlowableWorkerEndpoint} or subclass thereof
     */
    protected MethodFlowableWorkerEndpoint createMethodFlowableWorkerEndpoint() {
        return new MethodFlowableWorkerEndpoint();
    }

    protected String getEndpointId(FlowableWorker flowableWorker) {
        String id = flowableWorker.id();
        if (StringUtils.hasText(id)) {
            String resolvedId = resolve(id);
            return (resolvedId != null ? resolvedId : "");
        } else {
            return "org.flowable.external.client.FlowableWorkerEndpointContainer#" + this.counter.getAndIncrement();
        }
    }

    protected Duration resolveExpressionAsDuration(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof Duration duration) {
            return duration;
        } else if (resolved instanceof String str) {
            return Duration.parse(str);
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to a Duration or a String that can be parsed as a Duration. Resolved to [" + resolved.getClass()
                            + "] for [" + value + "]");
        }

        return null;
    }

    protected Integer resolveExpressionAsInteger(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof String str) {
            return Integer.parseInt(str);
        } else if (resolved instanceof Number number) {
            return number.intValue();
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to a Number or a String that can be parsed as an Integer. Resolved to [" + resolved.getClass()
                            + "] for [" + value + "]");
        }

        return null;
    }

    protected Object resolveExpression(String value) {
        return this.resolver.evaluate(resolve(value), this.expressionContext);
    }

    protected String resolve(String value) {
        if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
            return cbf.resolveEmbeddedValue(value);
        }
        return value;
    }

    /**
     * A {@link MessageHandlerMethodFactory} adapter that offers a configurable underlying instance to use.
     * Useful if the factory to use is determined once the endpoints have been registered but not created yet.
     *
     * @see FlowableWorkerEndpointRegistrar#setMessageHandlerMethodFactory
     */
    protected class MessageHandlerMethodFactoryAdapter implements MessageHandlerMethodFactory {

        protected MessageHandlerMethodFactory messageHandlerMethodFactory;

        public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
            this.messageHandlerMethodFactory = messageHandlerMethodFactory;
        }

        @Override
        public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
            return getMessageHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
        }

        protected MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
            if (this.messageHandlerMethodFactory == null) {
                this.messageHandlerMethodFactory = createDefaultHandlerMethodFactory();
            }
            return this.messageHandlerMethodFactory;
        }

        protected MessageHandlerMethodFactory createDefaultHandlerMethodFactory() {
            DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
            if (beanFactory != null) {
                defaultFactory.setBeanFactory(beanFactory);
            }
            defaultFactory.afterPropertiesSet();
            return defaultFactory;
        }
    }
}
