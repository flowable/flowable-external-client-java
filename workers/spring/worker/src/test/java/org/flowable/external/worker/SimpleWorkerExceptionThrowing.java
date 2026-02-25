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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
@Component
public class SimpleWorkerExceptionThrowing {

    protected final List<AcquiredExternalWorkerJob> receivedJobs = new CopyOnWriteArrayList<>();

    @FlowableWorker(topic = "simpleWithException")
    public void processJob(AcquiredExternalWorkerJob job) {
        try {
            Object exceptionMessage = job.getVariables().get("exceptionMessage");
            if (exceptionMessage instanceof String str) {
                throw new RuntimeException(str);
            }

            Object workerException = job.getVariables().get("workerException");
            if (workerException instanceof ObjectNode node) {
                String message = node.path("message").asText();
                FlowableWorkerException exception = new FlowableWorkerException(message);
                int retries = node.path("retries").asInt(-1);
                if (retries > 0) {
                    exception.setRetries(retries);
                }

                String details = node.path("details").asText(null);
                if (details != null) {
                    exception.setErrorDetails(details);
                }

                String retryTimeout = node.path("retryTimeout").asText(null);
                if (retryTimeout != null) {
                    exception.setRetryTimeout(Duration.parse(retryTimeout));
                }

                throw exception;
            }
        } finally {
            receivedJobs.add(job);
        }
    }

    public List<AcquiredExternalWorkerJob> getReceivedJobs() {
        return receivedJobs;
    }

    public void rest() {
        receivedJobs.clear();
    }
}
