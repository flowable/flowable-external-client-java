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
package org.flowable.external.worker.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;

import org.flowable.external.worker.annotation.FlowableWorkerConfigurer;

/**
 * Helper bean for registering {@link FlowableWorkerEndpoint} with a {@link FlowableWorkerEndpointRegistry}.
 *
 * @author Filip Hrisafov
 * @see FlowableWorkerConfigurer
 */
public class FlowableWorkerEndpointRegistrar implements BeanFactoryAware, InitializingBean {

	protected FlowableWorkerEndpointRegistry endpointRegistry;

	protected MessageHandlerMethodFactory messageHandlerMethodFactory;

	protected FlowableWorkerContainerFactory<?> containerFactory;

	protected String containerFactoryBeanName;

	protected BeanFactory beanFactory;

	protected final List<FlowableWorkerEndpointDescriptor> endpointDescriptors = new ArrayList<>();

	protected boolean startImmediately;

	protected Object mutex = this.endpointDescriptors;

	/**
	 * Set the {@link FlowableWorkerEndpointRegistry} instance to use.
	 */
	public void setEndpointRegistry(FlowableWorkerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * Return the {@link FlowableWorkerEndpointRegistry} instance for this
	 * registrar, may be {@code null}.
	 */
	public FlowableWorkerEndpointRegistry getEndpointRegistry() {
		return this.endpointRegistry;
	}

	/**
	 * Set the {@link MessageHandlerMethodFactory} to use to configure the job listener responsible to serve an endpoint detected by this processor.
	 * <p>By default, {@link DefaultMessageHandlerMethodFactory} is used and it can be configured further to support additional method arguments or to customize conversion and validation support.
	 * See {@link DefaultMessageHandlerMethodFactory} javadoc for more details.
	 */
	public void setMessageHandlerMethodFactory(@Nullable MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * Return the custom {@link MessageHandlerMethodFactory} to use, if any.
	 */
	@Nullable
	public MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
		return this.messageHandlerMethodFactory;
	}

	/**
	 * Set the {@link FlowableWorkerContainerFactory} to use in case a {@link FlowableWorkerEndpoint} is registered with a {@code null} container factory.
	 * <p>Alternatively, the bean name of the {@link FlowableWorkerContainerFactory} to use can be specified for a lazy lookup, see {@link #setContainerFactoryBeanName}.
	 */
	public void setContainerFactory(FlowableWorkerContainerFactory<?> containerFactory) {
		this.containerFactory = containerFactory;
	}

	/**
	 * Set the bean name of the {@link FlowableWorkerContainerFactory} to use in case a {@link FlowableWorkerEndpoint} is registered with a {@code null} container factory.
	 * Alternatively, the container factory instance can be registered directly: see {@link #setContainerFactory(FlowableWorkerContainerFactory)}.
	 *
	 * @see #setBeanFactory
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * A {@link BeanFactory} only needs to be available in conjunction with {@link #setContainerFactoryBeanName}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableBeanFactory cbf) {
			this.mutex = cbf.getSingletonMutex();
		}
	}

	@Override
	public void afterPropertiesSet() {
		registerAllEndpoints();
	}

	protected void registerAllEndpoints() {
		Assert.state(this.endpointRegistry != null, "No FlowableWorkerEndpointRegistry set");
		synchronized (this.mutex) {
			for (FlowableWorkerEndpointDescriptor descriptor : this.endpointDescriptors) {
				this.endpointRegistry.registerWorkerContainer(
						descriptor.endpoint, resolveContainerFactory(descriptor));
			}
			this.startImmediately = true;  // trigger immediate startup
		}
	}

	protected FlowableWorkerContainerFactory<?> resolveContainerFactory(FlowableWorkerEndpointDescriptor descriptor) {
		if (descriptor.containerFactory != null) {
			return descriptor.containerFactory;
		} else if (this.containerFactory != null) {
			return this.containerFactory;
		} else if (this.containerFactoryBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
			// Consider changing this if live change of the factory is required...
			this.containerFactory = this.beanFactory.getBean(this.containerFactoryBeanName, FlowableWorkerContainerFactory.class);
			return this.containerFactory;
		} else {
			throw new IllegalStateException("Could not resolve the " +
					FlowableWorkerContainerFactory.class.getSimpleName() + " to use for [" +
					descriptor.endpoint + "] no factory was given and no default is set.");
		}
	}

	/**
	 * Register a new {@link FlowableWorkerEndpoint} alongside the {@link FlowableWorkerContainerFactory} to use to create the underlying container.
	 * <p>The {@code factory} may be {@code null} if the default factory should be used for the supplied endpoint.
	 */
	public void registerEndpoint(FlowableWorkerEndpoint endpoint, @Nullable FlowableWorkerContainerFactory<?> factory) {
		Assert.notNull(endpoint, "Endpoint must not be null");
		Assert.hasText(endpoint.getId(), "Endpoint id must be set");

		// Factory may be null, we defer the resolution right before actually creating the container
		FlowableWorkerEndpointDescriptor descriptor = new FlowableWorkerEndpointDescriptor(endpoint, factory);

		synchronized (this.mutex) {
			if (this.startImmediately) {  // register and start immediately
				Assert.state(this.endpointRegistry != null, "No FlowableWorkerEndpointRegistry set");
				this.endpointRegistry.registerWorkerContainer(descriptor.endpoint, resolveContainerFactory(descriptor), true);
			} else {
				this.endpointDescriptors.add(descriptor);
			}
		}
	}

	/**
	 * Register a new {@link FlowableWorkerEndpoint} using the default
	 * {@link FlowableWorkerContainerFactory} to create the underlying container.
	 *
	 * @see #setContainerFactory(FlowableWorkerContainerFactory)
	 * @see #registerEndpoint(FlowableWorkerEndpoint, FlowableWorkerContainerFactory)
	 */
	public void registerEndpoint(FlowableWorkerEndpoint endpoint) {
		registerEndpoint(endpoint, null);
	}

	protected static class FlowableWorkerEndpointDescriptor {

		public final FlowableWorkerEndpoint endpoint;

		public final FlowableWorkerContainerFactory<?> containerFactory;

		public FlowableWorkerEndpointDescriptor(FlowableWorkerEndpoint endpoint, FlowableWorkerContainerFactory<?> containerFactory) {

			this.endpoint = endpoint;
			this.containerFactory = containerFactory;
		}
	}

}
