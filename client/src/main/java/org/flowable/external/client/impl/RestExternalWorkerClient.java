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
package org.flowable.external.client.impl;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.ExternalWorkerJobAcquireBuilder;
import org.flowable.external.client.ExternalWorkerJobCompletionBuilder;
import org.flowable.external.client.ExternalWorkerJobFailureBuilder;

import tools.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
public class RestExternalWorkerClient implements ExternalWorkerClient {

    protected final String workerId;
    protected final RestInvoker restInvoker;
    protected final ObjectMapper objectMapper;

    public RestExternalWorkerClient(String workerId, RestInvoker restInvoker, ObjectMapper objectMapper) {
        this.workerId = workerId;
        this.restInvoker = restInvoker;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder createJobAcquireBuilder() {
        return new RestExternalWorkerJobAcquireBuilder(workerId, restInvoker, objectMapper);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder createCompletionBuilder(AcquiredExternalWorkerJob job) {
        return new RestExternalWorkerCompletionBuilder(job, restInvoker, objectMapper);
    }

    @Override
    public ExternalWorkerJobFailureBuilder createFailureBuilder(AcquiredExternalWorkerJob job) {
        return new RestExternalWorkerFailureBuilder(job, restInvoker, objectMapper);
    }

    public static ExternalWorkerClient create(String workerId, RestInvoker restInvoker) {
        return create(workerId, restInvoker, new ObjectMapper());
    }

    public static ExternalWorkerClient create(String workerId, RestInvoker restInvoker, ObjectMapper objectMapper) {
        return new RestExternalWorkerClient(workerId, restInvoker, objectMapper);
    }

}
