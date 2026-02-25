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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Function;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerJobCompletionBuilder;
import org.flowable.external.client.FlowableClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

/**
 * @author Filip Hrisafov
 */
public class RestExternalWorkerCompletionBuilder implements ExternalWorkerJobCompletionBuilder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AcquiredExternalWorkerJob workerJob;
    protected final RestInvoker restInvoker;
    protected final ObjectMapper objectMapper;
    protected final ObjectNode request;

    public RestExternalWorkerCompletionBuilder(AcquiredExternalWorkerJob workerJob, RestInvoker restInvoker, ObjectMapper objectMapper) {
        this.workerJob = workerJob;
        this.restInvoker = restInvoker;
        this.objectMapper = objectMapper;
        this.request = objectMapper.createObjectNode()
                .put("workerId", workerJob.getWorkerId());
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, String value) {
        return addVariableToRequest(name, "string", value, StringNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Short value) {
        return addVariableToRequest(name, "short", value, ShortNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Integer value) {
        return addVariableToRequest(name, "integer", value, IntNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Long value) {
        return addVariableToRequest(name, "long", value, LongNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Double value) {
        return addVariableToRequest(name, "double", value, DoubleNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Boolean value) {
        return addVariableToRequest(name, "boolean", value, BooleanNode::valueOf);
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Date value) {
        return addVariableToRequest(name, "date", value, v -> StringNode.valueOf(v.toInstant().toString()));
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, Instant value) {
        return addVariableToRequest(name, "instant", value, v -> StringNode.valueOf(v.toString()));
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, LocalDate value) {
        return addVariableToRequest(name, "localDate", value, v -> StringNode.valueOf(v.toString()));
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, LocalDateTime value) {
        return addVariableToRequest(name, "localDateTime", value, v -> StringNode.valueOf(v.toString()));
    }

    @Override
    public ExternalWorkerJobCompletionBuilder variable(String name, JsonNode value) {
        return addVariableToRequest(name, "json", value, Function.identity());
    }

    @Override
    public ExternalWorkerJobCompletionBuilder convertAndAddJsonVariable(String name, Object value) {
        return addVariableToRequest(name, "json", value, this::convertToJson);
    }

    protected <V> ExternalWorkerJobCompletionBuilder addVariableToRequest(String name, String type, V value, Function<V, JsonNode> valueMapper) {
        request.withArray("variables")
                .addObject()
                .put("name", name)
                .put("type", type)
                .set("value", value != null ? valueMapper.apply(value) : NullNode.getInstance());
        return this;
    }

    protected JsonNode convertToJson(Object value) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            return objectMapper.readTree(serializedValue);
        } catch (JacksonException e) {
            throw new FlowableClientException("Failed to convert value of type " + value.getClass().getName() + " to json", e);
        }
    }

    @Override
    public void complete() {
        sendRequest("complete");
    }

    @Override
    public void bpmnError() {
        bpmnError(null);
    }

    @Override
    public void bpmnError(String errorCode) {
        if (!"bpmn".equals(workerJob.getScopeType())) {
            throw new FlowableClientException("Completing " + workerJob.getScopeType() + " external worker job with bpmn error is not allowed");
        }
        if (errorCode != null) {
            request.put("errorCode", errorCode);
        }
        sendRequest("bpmnError");
    }

    @Override
    public void cmmnTerminate() {
        if (!"cmmn".equals(workerJob.getScopeType())) {
            throw new FlowableClientException("Completing " + workerJob.getScopeType() + " external worker job with cmmn terminate is not allowed");
        }
        sendRequest("cmmnTerminate");
    }

    protected void sendRequest(String action) {
        String workerJobId = workerJob.getId();
        logger.debug("Sending complete request for job {} with worker {}", workerJobId, workerJob.getWorkerId());
        RestResponse<String> response = restInvoker.post("/acquire/jobs/" + workerJobId + "/" + action, request);
        int statusCode = response.statusCode();
        if (statusCode == 204) {
            logger.debug("Successfully completed job {}", workerJobId);
        } else {
            throw new FlowableClientException("Completing a job failed with status " + statusCode + " and body: " + response.body());
        }
    }
}
