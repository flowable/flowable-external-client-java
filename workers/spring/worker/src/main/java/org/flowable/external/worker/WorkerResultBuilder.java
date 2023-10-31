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

import org.flowable.external.worker.annotation.FlowableWorker;

/**
 * A builder that can be used to construct the appropriate {@link WorkerResult}.
 *
 * @author Filip Hrisafov
 */
public interface WorkerResultBuilder {

    /**
     * Create a new {@link WorkerResult.Success} that can be used as a return type
     * for methods annotated with {@link FlowableWorker @FlowableWorker}
     */
    WorkerResult.Success success();

    /**
     * Create a new {@link WorkerResult.Success} that can be used as a return type
     * for methods annotated with {@link FlowableWorker @FlowableWorker}.
     * This result will lead to a BPMN Error completion of the external worker.
     *
     * @see #bpmnError(String)
     */
    WorkerResult.Success bpmnError();

    /**
     * Create a new {@link WorkerResult.Success} that can be used as a return type
     * for methods annotated with {@link FlowableWorker @FlowableWorker}.
     * This result will lead to a BPMN Error completion of the external worker.
     *
     * @see #bpmnError()
     */
    WorkerResult.Success bpmnError(String errorCode);

    /**
     * Create a new {@link WorkerResult.Success} that can be used as a return type
     * for methods annotated with {@link FlowableWorker @FlowableWorker}
     * This result will lead to a CMMN termination of the external worker.
     */
    WorkerResult.Success cmmnTerminate();

    /**
     * Create a new {@link WorkerResult.Failure} that can be used as a return type
     * for methods annotated with {@link FlowableWorker @FlowableWorker}
     */
    WorkerResult.Failure failure();

}
