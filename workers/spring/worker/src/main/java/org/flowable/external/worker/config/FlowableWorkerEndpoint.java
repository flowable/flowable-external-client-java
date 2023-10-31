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
 * @author Filip Hrisafov
 */
public interface FlowableWorkerEndpoint {

    /**
     * Return the id of this endpoint.
     */
    String getId();

    /**
     * Set up the specified worker container with the model defined by this endpoint.
     * <p>This endpoint must provide the requested missing option(s) of
     * the specified container to make it usable. Usually, this is about
     * setting the {@code topic} and the {@code workerContainer} to
     * use but an implementation may override any default setting that
     * was already set.
     *
     * @param workerContainer the worker container to configure
     */
    void setupWorkerContainer(FlowableWorkerContainer workerContainer);
}
