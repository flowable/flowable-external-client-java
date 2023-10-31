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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.config.DefaultFlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;
import org.flowable.external.worker.worker.FlowableWorkerContainer;
import org.springframework.context.annotation.Import;

import org.flowable.external.worker.config.FlowableWorkerEndpointRegistrar;
import org.flowable.external.worker.config.FlowableWorkerEndpointRegistry;

/**
 * Enable Flowable Worker annotated endpoints that are created under the cover by a {@link FlowableWorkerContainerFactory FlowableWorkerContainerFactory}.
 * To be used on a {@link org.springframework.context.annotation.Configuration @Configuration} classes as follows:
 *
 * <pre><code>
 * &#064;Configuration
 * &#064;EnableFlowableWorker
 * public class AppConfig {
 *
 *    &#064;Bean
 *    public DefaultFlowableWorkerContainerFactory flowableWorkerContainerFactory(ExternalWorkerClient externalWorkerClient) {
 *       DefaultFlowableWorkerContainerFactory factory = new DefaultFlowableWorkerContainerFactory();
 *       factory.setExternalWorkerClient(externalWorkerClient);
 *       return factory;
 *    }
 * }</code></pre>
 * <p>The {@code FlowableWorkerContainerFactory} is responsible for creating the worker container responsible for a particular endpoint.
 * Typical implementations as the {@link DefaultFlowableWorkerContainerFactory DefaultFlowableWorkerContainerFactory}
 * used in the sample above, provide the necessary configuration options that are supported by the underlying {@link FlowableWorkerContainer FlowableWorkerContainer}.
 *
 * <p>{@code @EnableFlowableWorker} enables the detction of {@link FlowableWorker @FlowableWorker} annotations
 * on any Spring-managed bean in the container.
 * For example given a class {@code DemoWorker}
 *
 * <pre><code>
 * package com.example.demo
 *
 * &#064;Component
 * public class DemoWorker {
 *
 *     &#064;FlowableWorker(topic = "customer")
 *     public void process(AcquiredExternalWorkerJob job) {
 *         // Process the job
 *     }
 * }</code></pre>
 *
 * <p>Annotated methods can use flexible signature; in particular, it is possible to use {@link WorkerResult WorkerResult}
 * to control whether the job processing has been successful or not. See {@link FlowableWorker @FlowableWorker} javadoc for more details.
 *
 * @author Filip Hrisafov
 * @see FlowableWorker
 * @see FlowableWorkerAnnotationBeanPostProcessor
 * @see FlowableWorkerEndpointRegistrar
 * @see FlowableWorkerEndpointRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FlowableWorkerBootstrapConfiguration.class)
public @interface EnableFlowableWorker {

}
