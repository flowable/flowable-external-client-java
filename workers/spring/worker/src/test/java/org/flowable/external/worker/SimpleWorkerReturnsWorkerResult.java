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
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
@Component
public class SimpleWorkerReturnsWorkerResult {

    protected final List<AcquiredExternalWorkerJob> receivedJobs = new CopyOnWriteArrayList<>();
    protected final ObjectMapper objectMapper;

    public SimpleWorkerReturnsWorkerResult(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @FlowableWorker(topic = "simpleWithReturnResult")
    public WorkerResult processJob(AcquiredExternalWorkerJob job, WorkerResultBuilder resultBuilder) {
        try {

            Object workerFailure = job.getVariables().get("workerFailure");
            if (workerFailure instanceof ObjectNode node) {
                String message = node.path("message").asText();
                WorkerResult.Failure failure = resultBuilder.failure().message(message);
                int retries = node.path("retries").asInt(-1);
                if (retries > 0) {
                    failure.retries(retries);
                }

                String details = node.path("details").asText(null);
                if (details != null) {
                    failure.details(details);
                }

                String retryTimeout = node.path("retryTimeout").asText(null);
                if (retryTimeout != null) {
                    failure.retryTimeout(Duration.parse(retryTimeout));
                }

                return failure;
            }

            WorkerResult.Success result;
            Object successType = job.getVariables().get("successType");
            if (successType == null) {
                result = resultBuilder.success();
            } else if ("bpmnError".equals(successType)) {
                Object errorCode = job.getVariables().get("errorCode");
                if (errorCode != null) {
                    result = resultBuilder.bpmnError(errorCode.toString());
                } else {
                    result = resultBuilder.bpmnError();
                }
            } else if ("cmmnTerminate".equals(successType)) {
                result = resultBuilder.cmmnTerminate();
            } else {
                throw new IllegalStateException("Unexpected value: " + successType);
            }

            return result
                    .variable("stringVar", "String value")
                    .variable("shortVar", (short) 12)
                    .variable("integerVar", 42)
                    .variable("longVar", 4242L)
                    .variable("doubleVar", 42.42d)
                    .variable("booleanVar", true)
                    .variable("dateVar", Date.from(Instant.parse("2023-08-23T08:45:12Z")))
                    .variable("instantVar", Instant.parse("2023-08-23T10:15:27Z"))
                    .variable("localDateVar", LocalDate.of(2023, Month.AUGUST, 22))
                    .variable("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 22).atTime(8, 34, 12))
                    .variable("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit"))
                    .variable("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2))
                    .convertAndAddJsonVariable("jsonMapVar", Map.of("name", "Fozzie", "age", 25))
                    .convertAndAddJsonVariable("jsonListVar", List.of("Kermit", "Fozzie"));
        } finally {
            receivedJobs.add(job);
        }

    }

    public List<AcquiredExternalWorkerJob> getReceivedJobs() {
        return receivedJobs;
    }
}
