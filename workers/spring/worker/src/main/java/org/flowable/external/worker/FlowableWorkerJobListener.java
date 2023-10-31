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
package org.flowable.external.worker;

import org.flowable.external.client.AcquiredExternalWorkerJob;

/**
 * Listener interface to receive acquire Flowable External Worker jobs
 *
 * @author Filip Hrisafov
 */
@FunctionalInterface
public interface FlowableWorkerJobListener {

    void onAcquiredJob(AcquiredExternalWorkerJob job);

    default void onAcquiredJob(AcquiredExternalWorkerJob job, WorkerContext workerContext) {
        throw new UnsupportedOperationException("Container should never call this");
    }
}
