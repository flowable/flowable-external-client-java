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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerJobFailureBuilder;
import org.flowable.external.client.FlowableClientException;

/**
 * @author Filip Hrisafov
 */
public class RestExternalWorkerFailureBuilder implements ExternalWorkerJobFailureBuilder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AcquiredExternalWorkerJob workerJob;
    protected final RestInvoker restInvoker;
    protected final ObjectNode request;

    public RestExternalWorkerFailureBuilder(AcquiredExternalWorkerJob workerJob, RestInvoker restInvoker, ObjectMapper objectMapper) {
        this.workerJob = workerJob;
        this.restInvoker = restInvoker;
        this.request = objectMapper.createObjectNode()
                .put("workerId", workerJob.getWorkerId());
    }

    @Override
    public ExternalWorkerJobFailureBuilder error(Exception exception) {
        message(exception.getMessage());
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            exception.printStackTrace(pw);
            details(sw.getBuffer().toString());
            return this;
        } catch (IOException e) {
            // This should never happen since StringWriter does not do anything on close
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ExternalWorkerJobFailureBuilder message(String message) {
        request.put("errorMessage", message);
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder details(String details) {
        request.put("errorDetails", details);
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder retries(int retries) {
        request.put("retries", retries);
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder retryTimeout(Duration retryTimeout) {
        request.put("retryTimeout", retryTimeout.toString());
        return this;
    }

    @Override
    public void fail() {
        sendRequest();
    }

    protected void sendRequest() {
        String workerJobId = workerJob.getId();
        logger.debug("Sending fail request for job {} with worker {}", workerJobId, workerJob.getWorkerId());
        RestResponse<String> response = restInvoker.post("/acquire/jobs/" + workerJobId + "/fail", request);
        int statusCode = response.statusCode();
        if (statusCode == 204) {
            logger.debug("Successfully failed job {}", workerJobId);
        } else {
            throw new FlowableClientException("Failing a job failed with status " + statusCode + " and body: " + response.body());
        }
    }
}
