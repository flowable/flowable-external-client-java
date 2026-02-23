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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.FlowableClientException;

/**
 * @author Filip Hrisafov
 */
public class RestExternalWorkerJobAcquireBuilder extends BaseExternalWorkerJobAcquireBuilder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String workerId;
    protected final RestInvoker restInvoker;
    protected final ObjectMapper objectMapper;

    public RestExternalWorkerJobAcquireBuilder(String workerId, RestInvoker restInvoker, ObjectMapper objectMapper) {
        this.workerId = workerId;
        this.restInvoker = restInvoker;
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<AcquiredExternalWorkerJob> acquireAndLockInternal() {
        ObjectNode request = prepareRequest();
        return sendRequestAndAcquire(request);
    }

    protected ObjectNode prepareRequest() {
        ObjectNode request = objectMapper.createObjectNode()
                .put("topic", topic)
                .put("workerId", workerId);

        if (lockDuration != null) {
            request.put("lockDuration", lockDuration.toString());
        }

        if (scopeType != null) {
            request.put("scopeType", scopeType);
        }

        if (numberOfTasks > 0) {
            request.put("numberOfTasks", numberOfTasks);
        }

        if (numberOfRetries > 0) {
            request.put("numberOfRetries", numberOfRetries);
        }

        return request;
    }

    protected List<AcquiredExternalWorkerJob> sendRequestAndAcquire(ObjectNode requestBody) {
        logger.debug("Acquiring jobs for worker {}", workerId);
        RestResponse<String> response = restInvoker.post("/acquire/jobs", requestBody);
        int statusCode = response.statusCode();
        String responseBody = response.body();
        if (statusCode == 200) {
            logger.debug("Acquired jobs for worker {}. Response: {}", workerId, responseBody);
            return asList(responseBody);
        } else {
            throw new FlowableClientException("Acquiring jobs failed with status " + statusCode + " and body: " + responseBody);
        }
    }

    protected List<AcquiredExternalWorkerJob> asList(String response) {
        try {
            ArrayNode jobsNode = objectMapper.readValue(response, ArrayNode.class);
            List<AcquiredExternalWorkerJob> jobs = new ArrayList<>(jobsNode.size());
            for (JsonNode jobNode : jobsNode) {
                if (jobNode.isObject()) {
                    jobs.add(asJob((ObjectNode) jobNode));
                }
            }

            return jobs;
        } catch (JacksonException e) {
            throw new FlowableClientException("Failed to read response", e);
        }
    }

    protected AcquiredExternalWorkerJob asJob(ObjectNode jobNode) {
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId(jobNode.path("id").asText(null));
        job.setCorrelationId(jobNode.path("correlationId").asText(null));
        job.setRetries(jobNode.path("retries").asInt(0));

        if (jobNode.hasNonNull("processInstanceId")) {
            job.setScopeId(jobNode.path("processInstanceId").asText(null));
            job.setScopeType("bpmn");
            job.setSubScopeId(jobNode.path("executionId").asText(null));
            job.setScopeDefinitionId(jobNode.path("processDefinitionId").asText(null));
        } else {
            job.setScopeId(jobNode.path("scopeId").asText(null));
            job.setScopeType(jobNode.path("scopeType").asText(null));
            job.setSubScopeId(jobNode.path("subScopeId").asText(null));
            job.setScopeDefinitionId(jobNode.path("scopeDefinitionId").asText(null));
        }

        job.setTenantId(jobNode.path("tenantId").asText(null));

        job.setElementId(jobNode.path("elementId").asText(null));
        job.setElementName(jobNode.path("elementName").asText(null));

        job.setExceptionMessage(jobNode.path("exceptionMessage").asText(null));

        job.setCreateTime(asInstant(jobNode.path("createTime").asText(null)));
        job.setDueDate(asInstant(jobNode.path("dueDate").asText(null)));

        job.setWorkerId(jobNode.path("lockOwner").asText(null));
        job.setLockExpirationTime(asInstant(jobNode.path("lockExpirationTime").asText(null)));

        JsonNode variablesNode = jobNode.path("variables");
        if (variablesNode.isArray() && !variablesNode.isEmpty()) {
            parseVariables((ArrayNode) variablesNode, job::addVariable);
        }

        return job;
    }

    protected Instant asInstant(String value) {
        if (value == null) {
            return null;
        }

        return Instant.parse(value);
    }

    protected void parseVariables(ArrayNode variablesNode, BiConsumer<String, Object> variableConsumer) {
        for (JsonNode variableNode : variablesNode) {

            String variableName = variableNode.path("name").asText(null);
            if (variableName == null || variableName.isEmpty()) {
                continue;
            }

            String type = variableNode.path("type").asText(null);
            JsonNode valueNode = variableNode.path("value");

            variableConsumer.accept(variableName, parseVariableValue(type, valueNode));
        }
    }

    protected Object parseVariableValue(String type, JsonNode valueNode) {
        if (valueNode.isMissingNode() || valueNode.isNull() || type == null) {
            return null;
        }

        return switch (type) {
            case "string" -> valueNode.textValue();
            case "json" -> valueNode;
            case "boolean" -> valueNode.booleanValue();
            case "double" -> valueNode.doubleValue();
            case "long" -> valueNode.longValue();
            case "date" -> Date.from(Instant.parse(valueNode.textValue()));
            case "instant" -> Instant.parse(valueNode.textValue());
            case "localDate" -> LocalDate.parse(valueNode.textValue());
            case "localDateTime" -> LocalDateTime.parse(valueNode.textValue());
            case "short" -> valueNode.shortValue();
            case "integer" -> valueNode.intValue();
            default -> {
                logger.warn("Cannot parse variable type {}", type);
                yield null;
            }
        };
    }
}
