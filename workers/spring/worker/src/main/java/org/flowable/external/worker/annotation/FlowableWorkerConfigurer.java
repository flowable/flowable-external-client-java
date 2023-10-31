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

import org.flowable.external.worker.config.FlowableWorkerEndpointRegistrar;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerEndpoint;
import org.flowable.external.worker.config.FlowableWorkerEndpointRegistry;

/**
 * Optional interface to be implemented by a Spring managed bean willing to customize how Flowable worker endpoints are configured.
 * Typically used to define the default {@link FlowableWorkerContainerFactory FlowableWorkerContainerFactory}
 * to use or for registering Flowable Worker endpoints in a <em>programmatic</em> fashion as opposed to the <em>declarative</em>
 * approach of using the {@link FlowableWorker @FlowableWorker} annotation.
 *
 * <p>See {@link EnableFlowableWorker @EnableFlowableWorker} for detailed usage examples.
 *
 * @author Filip Hrisafov
 * @see EnableFlowableWorker
 * @see FlowableWorkerEndpointRegistrar
 */
@FunctionalInterface
public interface FlowableWorkerConfigurer {

	/**
	 * Callback allowing a {@link FlowableWorkerEndpointRegistry FlowableWorkerEndpointRegistry}
	 * and specific {@link FlowableWorkerEndpoint FlowableWorkerEndpoint}
	 * instances to be registered against the given {@link FlowableWorkerEndpointRegistrar}.
	 * The default {@link FlowableWorkerContainerFactory FlowableWorkerContainerFactory} can also be customized.
	 *
	 * @param registrar the registrar to be configured
	 */
	void configureFlowableWorkers(FlowableWorkerEndpointRegistrar registrar);

}
