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

import org.flowable.external.worker.worker.FlowableWorkerContainer;

/**
 * Factory of {@link FlowableWorkerContainer} based on a {@link FlowableWorkerEndpoint} definition.
 *
 * @param <C> the container type
 * @author Filip Hrisafov
 * @see FlowableWorkerEndpoint
 */
public interface FlowableWorkerContainerFactory<C extends FlowableWorkerContainer> {

    /**
     * Create a {@link FlowableWorkerContainer} for the given {@link FlowableWorkerEndpoint}.
     *
     * @param endpoint the endpoint to configure
     * @return the created container
     */
    C createWorkerContainer(FlowableWorkerEndpoint endpoint);
}
