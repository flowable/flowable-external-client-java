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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.ExternalWorkerJob;
import org.flowable.external.client.FlowableClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ExceptionUtils;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
class RestExternalWorkerClientTest {

    protected ExternalWorkerClient client;
    protected TestRestInvoker restInvoker;
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restInvoker = new TestRestInvoker();
        objectMapper = new ObjectMapper();
        client = RestExternalWorkerClient.create("test-worker", restInvoker, objectMapper);
    }

    @Test
    void acquireJobsMinimalRequest() {
        restInvoker.response = new TestRestResponse(200, """
                [
                  {
                    "id": "job-1",
                    "correlationId": "correlation-1",
                    "processInstanceId": "proc-1",
                    "executionId": "exec-1",
                    "processDefinitionId": "proc-def-1",
                    "tenantId": "flowable",
                    "elementId": "elem-1",
                    "elementName": "Element 1",
                    "createTime": "2023-08-21T13:12:28Z",
                    "dueDate": "2023-08-25T12:24:28Z",
                    "retries": 4,
                    "lockOwner": "tester-worker",
                    "lockExpirationTime": "2023-08-22T10:12:28Z",
                    "variables": [
                      { "name": "stringVar", "type": "string", "value": "String value" },
                      { "name": "jsonArrayVar", "type": "json", "value": [ 1, 2 ] },
                      { "name": "jsonObjectVar", "type": "json", "value": { "name": "Kermit" } },
                      { "name": "booleanVar", "type": "boolean", "value": true },
                      { "name": "doubleVar", "type": "double", "value": 42.5 },
                      { "name": "longVar", "type": "long", "value": 12345 },
                      { "name": "shortVar", "type": "short", "value": 12 },
                      { "name": "integerVar", "type": "integer", "value": 24 },
                      { "name": "dateVar", "type": "date", "value": "2023-08-21T16:05:12Z" },
                      { "name": "instantVar", "type": "instant", "value": "2023-08-20T10:12:28Z" },
                      { "name": "localDateVar", "type": "localDate", "value": "2023-08-21" },
                      { "name": "localDateTimeVar", "type": "localDateTime", "value": "2023-08-21T17:15:35" },
                      { "name": "nullVar", "value": null }
                    ]
                  }
                ]
                """);
        List<AcquiredExternalWorkerJob> jobs = client.createJobAcquireBuilder().topic("customer").acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  topic: 'customer',"
                        + "  workerId: 'test-worker'"
                        + "}");
        assertThat(jobs).hasSize(1);
        AcquiredExternalWorkerJob job = jobs.get(0);
        assertThat(job.getId()).isEqualTo("job-1");
        assertThat(job.getCorrelationId()).isEqualTo("correlation-1");
        assertThat(job.getScopeId()).isEqualTo("proc-1");
        assertThat(job.getScopeType()).isEqualTo("bpmn");
        assertThat(job.getSubScopeId()).isEqualTo("exec-1");
        assertThat(job.getScopeDefinitionId()).isEqualTo("proc-def-1");
        assertThat(job.getTenantId()).isEqualTo("flowable");
        assertThat(job.getElementId()).isEqualTo("elem-1");
        assertThat(job.getElementName()).isEqualTo("Element 1");
        assertThat(job.getCreateTime()).isEqualTo(Instant.parse("2023-08-21T13:12:28Z"));
        assertThat(job.getDueDate()).isEqualTo(Instant.parse("2023-08-25T12:24:28Z"));
        assertThat(job.getWorkerId()).isEqualTo("tester-worker");
        assertThat(job.getRetries()).isEqualTo(4);
        assertThat(job.getLockExpirationTime()).isEqualTo(Instant.parse("2023-08-22T10:12:28Z"));
        assertThat(job.getVariables())
                .containsOnly(
                        entry("stringVar", "String value"),
                        entry("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2)),
                        entry("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit")),
                        entry("booleanVar", true),
                        entry("doubleVar", 42.5d),
                        entry("longVar", 12345L),
                        entry("integerVar", 24),
                        entry("shortVar", (short) 12),
                        entry("dateVar", Date.from(Instant.parse("2023-08-21T16:05:12Z"))),
                        entry("instantVar", Instant.parse("2023-08-20T10:12:28Z")),
                        entry("localDateVar", LocalDate.of(2023, Month.AUGUST, 21)),
                        entry("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 21).atTime(17, 15, 35)),
                        entry("nullVar", null)
                );
    }

    @Test
    void acquireMultipleJobsMinimalRequest() {
        restInvoker.response = new TestRestResponse(200, """
                [
                  {
                    "id": "job-1",
                    "correlationId": "correlation-1",
                    "processInstanceId": "proc-1",
                    "executionId": "exec-1",
                    "processDefinitionId": "proc-def-1",
                    "tenantId": "flowable"
                  },
                  {
                    "id": "job-2",
                    "correlationId": "correlation-2",
                    "scopeId": "case-1",
                    "subScopeId": "plan-item-1",
                    "scopeDefinitionId": "case-def-1",
                    "scopeType": "cmmn",
                    "tenantId": "flowable"
                  }
                ]
                """);
        List<AcquiredExternalWorkerJob> jobs = client.createJobAcquireBuilder().topic("customer").acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  topic: 'customer',"
                        + "  workerId: 'test-worker'"
                        + "}");
        assertThat(jobs)
                .extracting(ExternalWorkerJob::getId, ExternalWorkerJob::getScopeType, ExternalWorkerJob::getScopeId, ExternalWorkerJob::getSubScopeId,
                        ExternalWorkerJob::getScopeDefinitionId)
                .containsExactly(
                        tuple("job-1", "bpmn", "proc-1", "exec-1", "proc-def-1"),
                        tuple("job-2", "cmmn", "case-1", "plan-item-1", "case-def-1")
                );
    }

    @Test
    void acquireJobsWithAllParameters() {
        restInvoker.response = new TestRestResponse(200, "[]");
        client.createJobAcquireBuilder()
                .topic("customer")
                .lockDuration(Duration.ofMinutes(1))
                .numberOfTasks(4)
                .numberOfRetries(3)
                .acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          topic: 'customer',
                          workerId: 'test-worker',
                          lockDuration: 'PT1M',
                          numberOfTasks: 4,
                          numberOfRetries: 3
                        }
                        """);
    }

    @Test
    void acquireOnlyCmmnJobs() {
        restInvoker.response = new TestRestResponse(200, "[]");
        client.createJobAcquireBuilder()
                .topic("customer")
                .onlyCmmn()
                .acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          topic: 'customer',
                          workerId: 'test-worker',
                          scopeType: 'cmmn'
                        }
                        """);
    }

    @Test
    void acquireOnlyBpmnJobs() {
        restInvoker.response = new TestRestResponse(200, "[]");
        client.createJobAcquireBuilder()
                .topic("customer")
                .onlyBpmn()
                .acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          topic: 'customer',
                          workerId: 'test-worker',
                          scopeType: 'bpmn'
                        }
                        """);
    }

    @Test
    void acquireCustomScopeTypeJobs() {
        restInvoker.response = new TestRestResponse(200, "[]");
        client.createJobAcquireBuilder()
                .topic("customer")
                .scopeType("dummy")
                .acquireAndLock();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          topic: 'customer',
                          workerId: 'test-worker',
                          scopeType: 'dummy'
                        }
                        """);
    }

    @ParameterizedTest
    @ValueSource(ints = { 201, 400, 404, 500 })
    void acquireJobsReturnsInvalidStatusCode(int statusCode) {
        String responseBody = "{ \"statusCode\": " + statusCode + " }";
        restInvoker.response = new TestRestResponse(statusCode, responseBody);

        assertThatThrownBy(() -> client.createJobAcquireBuilder()
                .topic("customer")
                .acquireAndLock())
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Acquiring jobs failed with status " + statusCode + " and body: " + responseBody);
    }

    @Test
    void acquireJobsWithInvalidArguments() {
        assertThatThrownBy(() -> client.createJobAcquireBuilder().acquireAndLock())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic has to be provided");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().topic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic is empty");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().topic(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic is empty");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().lockDuration(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lockDuration is null");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().lockDuration(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lockDuration must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().lockDuration(Duration.ofSeconds(-10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lockDuration must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().numberOfRetries(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("numberOfRetries must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().numberOfRetries(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("numberOfRetries must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().numberOfTasks(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("numberOfTasks must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().numberOfTasks(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("numberOfTasks must be positive");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().onlyBpmn().onlyCmmn())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot combine onlyBpmn() with onlyCmmn() in the same query");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().scopeType("dummy").onlyCmmn())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot combine scopeType(String) with onlyCmmn() in the same query");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().onlyCmmn().onlyBpmn())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot combine onlyCmmn() with onlyBpmn() in the same query");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().scopeType("dummy").onlyBpmn())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot combine scopeType(String) with onlyBpmn() in the same query");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().scopeType(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scopeType is empty");

        assertThatThrownBy(() -> client.createJobAcquireBuilder().scopeType(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scopeType is empty");
    }

    @Test
    void completeJobMinimalRequest() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createCompletionBuilder(job).complete();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/complete");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  workerId: 'worker'"
                        + "}");
    }

    @Test
    void completeJobWithAllVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createCompletionBuilder(job)
                .variable("stringVar", "String value")
                .variable("shortVar", (short) 12)
                .variable("integerVar", 42)
                .variable("longVar", 4242L)
                .variable("doubleVar", 42.42d)
                .variable("booleanVar", true)
                .variable("dateVar", Date.from(Instant.parse("2023-08-22T08:45:12Z")))
                .variable("instantVar", Instant.parse("2023-08-22T10:15:27Z"))
                .variable("localDateVar", LocalDate.of(2023, Month.AUGUST, 21))
                .variable("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 21).atTime(8, 34, 12))
                .complete();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/complete");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'stringVar', type: 'string', value: 'String value' },
                            { name: 'shortVar', type: 'short', value: 12 },
                            { name: 'integerVar', type: 'integer', value: 42 },
                            { name: 'longVar', type: 'long', value: 4242 },
                            { name: 'doubleVar', type: 'double', value: 42.42 },
                            { name: 'booleanVar', type: 'boolean', value: true },
                            { name: 'dateVar', type: 'date', value: '2023-08-22T08:45:12Z' },
                            { name: 'instantVar', type: 'instant', value: '2023-08-22T10:15:27Z' },
                            { name: 'localDateVar', type: 'localDate', value: '2023-08-21' },
                            { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-21T08:34:12' }
                          ]
                        }
                        """);
    }

    @Test
    void completeJobWithJsonVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createCompletionBuilder(job)
                .variable("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit"))
                .variable("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2))
                .complete();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/complete");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                            { name: 'jsonArrayVar', type: 'json', value: [1, 2] }
                          ]
                        }
                        """);
    }

    @Test
    void completeJobWithConvertedVariable() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createCompletionBuilder(job)
                .convertAndAddJsonVariable("jsonMapVar", Map.of("name", "Kermit", "age", 30))
                .convertAndAddJsonVariable("jsonListVar", List.of(1, 2))
                .complete();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/complete");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonMapVar', type: 'json', value: { name: 'Kermit', age: 30 } },
                            { name: 'jsonListVar', type: 'json', value: [1, 2] }
                          ]
                        }
                        """);
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 400, 404, 500 })
    void completeJobReturnsInvalidStatusCode(int statusCode) {
        String responseBody = "{ \"statusCode\": " + statusCode + " }";
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        restInvoker.response = new TestRestResponse(statusCode, responseBody);

        assertThatThrownBy(() -> client.createCompletionBuilder(job).complete())
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Completing a job failed with status " + statusCode + " and body: " + responseBody);
    }

    @Test
    void bpmnErrorJobMinimalRequest() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job).bpmnError();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  workerId: 'worker'"
                        + "}");
    }

    @Test
    void bpmnErrorJobWithAllVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .variable("stringVar", "String value")
                .variable("shortVar", (short) 12)
                .variable("integerVar", 42)
                .variable("longVar", 4242L)
                .variable("doubleVar", 42.42d)
                .variable("booleanVar", true)
                .variable("dateVar", Date.from(Instant.parse("2023-08-22T08:45:12Z")))
                .variable("instantVar", Instant.parse("2023-08-22T10:15:27Z"))
                .variable("localDateVar", LocalDate.of(2023, Month.AUGUST, 21))
                .variable("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 21).atTime(8, 34, 12))
                .bpmnError();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'stringVar', type: 'string', value: 'String value' },
                            { name: 'shortVar', type: 'short', value: 12 },
                            { name: 'integerVar', type: 'integer', value: 42 },
                            { name: 'longVar', type: 'long', value: 4242 },
                            { name: 'doubleVar', type: 'double', value: 42.42 },
                            { name: 'booleanVar', type: 'boolean', value: true },
                            { name: 'dateVar', type: 'date', value: '2023-08-22T08:45:12Z' },
                            { name: 'instantVar', type: 'instant', value: '2023-08-22T10:15:27Z' },
                            { name: 'localDateVar', type: 'localDate', value: '2023-08-21' },
                            { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-21T08:34:12' }
                          ]
                        }
                        """);
    }

    @Test
    void bpmnErrorJobWithJsonVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .variable("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit"))
                .variable("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2))
                .bpmnError();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                            { name: 'jsonArrayVar', type: 'json', value: [1, 2] }
                          ]
                        }
                        """);
    }

    @Test
    void bpmnErrorJobWithConvertedVariable() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .convertAndAddJsonVariable("jsonMapVar", Map.of("name", "Kermit", "age", 30))
                .convertAndAddJsonVariable("jsonListVar", List.of(1, 2))
                .bpmnError();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonMapVar', type: 'json', value: { name: 'Kermit', age: 30 } },
                            { name: 'jsonListVar', type: 'json', value: [1, 2] }
                          ]
                        }
                        """);
    }

    @Test
    void bpmnErrorJobWithInvalidScopeType() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("cmmn");
        assertThatThrownBy(() -> client.createCompletionBuilder(job)
                .bpmnError("testError"))
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Completing cmmn external worker job with bpmn error is not allowed");
    }

    @Test
    void bpmnErrorWithErrorCodeJobMinimalRequest() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job).bpmnError("testError");

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  workerId: 'worker',"
                        + "  errorCode: 'testError'"
                        + "}");
    }

    @Test
    void bpmnErrorWithErrorCodeJobWithAllVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .variable("stringVar", "String value")
                .variable("shortVar", (short) 12)
                .variable("integerVar", 42)
                .variable("longVar", 4242L)
                .variable("doubleVar", 42.42d)
                .variable("booleanVar", true)
                .variable("dateVar", Date.from(Instant.parse("2023-08-22T08:45:12Z")))
                .variable("instantVar", Instant.parse("2023-08-22T10:15:27Z"))
                .variable("localDateVar", LocalDate.of(2023, Month.AUGUST, 21))
                .variable("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 21).atTime(8, 34, 12))
                .bpmnError("testError");

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'stringVar', type: 'string', value: 'String value' },
                            { name: 'shortVar', type: 'short', value: 12 },
                            { name: 'integerVar', type: 'integer', value: 42 },
                            { name: 'longVar', type: 'long', value: 4242 },
                            { name: 'doubleVar', type: 'double', value: 42.42 },
                            { name: 'booleanVar', type: 'boolean', value: true },
                            { name: 'dateVar', type: 'date', value: '2023-08-22T08:45:12Z' },
                            { name: 'instantVar', type: 'instant', value: '2023-08-22T10:15:27Z' },
                            { name: 'localDateVar', type: 'localDate', value: '2023-08-21' },
                            { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-21T08:34:12' }
                          ],
                          errorCode: 'testError'
                        }
                        """);
    }

    @Test
    void bpmnErrorWithErrorCodeJobWithJsonVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .variable("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit"))
                .variable("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2))
                .bpmnError("testError");

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                            { name: 'jsonArrayVar', type: 'json', value: [1, 2] }
                          ],
                          errorCode: 'testError'
                        }
                        """);
    }

    @Test
    void bpmnErrorWithErrorCodeJobWithConvertedVariable() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        client.createCompletionBuilder(job)
                .convertAndAddJsonVariable("jsonMapVar", Map.of("name", "Kermit", "age", 30))
                .convertAndAddJsonVariable("jsonListVar", List.of(1, 2))
                .bpmnError("testError");

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/bpmnError");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonMapVar', type: 'json', value: { name: 'Kermit', age: 30 } },
                            { name: 'jsonListVar', type: 'json', value: [1, 2] }
                          ],
                          errorCode: 'testError'
                        }
                        """);
    }

    @Test
    void bpmnErrorWithErrorCodeJobWithInvalidScopeType() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("cmmn");
        assertThatThrownBy(() -> client.createCompletionBuilder(job)
                .bpmnError())
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Completing cmmn external worker job with bpmn error is not allowed");
    }

    @Test
    void cmmnTerminateJobMinimalRequest() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("cmmn");
        client.createCompletionBuilder(job).cmmnTerminate();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/cmmnTerminate");
        assertThatJson(restInvoker.body)
                .isEqualTo("{"
                        + "  workerId: 'worker'"
                        + "}");
    }

    @Test
    void cmmnTerminateJobWithAllVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("cmmn");
        client.createCompletionBuilder(job)
                .variable("stringVar", "String value")
                .variable("shortVar", (short) 12)
                .variable("integerVar", 42)
                .variable("longVar", 4242L)
                .variable("doubleVar", 42.42d)
                .variable("booleanVar", true)
                .variable("dateVar", Date.from(Instant.parse("2023-08-22T08:45:12Z")))
                .variable("instantVar", Instant.parse("2023-08-22T10:15:27Z"))
                .variable("localDateVar", LocalDate.of(2023, Month.AUGUST, 21))
                .variable("localDateTimeVar", LocalDate.of(2023, Month.AUGUST, 21).atTime(8, 34, 12))
                .cmmnTerminate();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/cmmnTerminate");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'stringVar', type: 'string', value: 'String value' },
                            { name: 'shortVar', type: 'short', value: 12 },
                            { name: 'integerVar', type: 'integer', value: 42 },
                            { name: 'longVar', type: 'long', value: 4242 },
                            { name: 'doubleVar', type: 'double', value: 42.42 },
                            { name: 'booleanVar', type: 'boolean', value: true },
                            { name: 'dateVar', type: 'date', value: '2023-08-22T08:45:12Z' },
                            { name: 'instantVar', type: 'instant', value: '2023-08-22T10:15:27Z' },
                            { name: 'localDateVar', type: 'localDate', value: '2023-08-21' },
                            { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-21T08:34:12' }
                          ]
                        }
                        """);
    }

    @Test
    void cmmnTerminateJobWithJsonVariables() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("cmmn");
        client.createCompletionBuilder(job)
                .variable("jsonObjectVar", objectMapper.createObjectNode().put("name", "Kermit"))
                .variable("jsonArrayVar", objectMapper.createArrayNode().add(1).add(2))
                .cmmnTerminate();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/cmmnTerminate");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          variables: [
                            { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                            { name: 'jsonArrayVar', type: 'json', value: [1, 2] }
                          ]
                        }
                        """);
    }

    @Test
    void cmmnTerminateJobWithInvalidScopeType() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        job.setScopeType("bpmn");
        assertThatThrownBy(() -> client.createCompletionBuilder(job)
                .cmmnTerminate())
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Completing bpmn external worker job with cmmn terminate is not allowed");
    }

    @Test
    void failJobWithMinimalRequest() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createFailureBuilder(job).fail();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/fail");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker'
                        }
                        """);
    }

    @Test
    void failJobWithRetryInformation() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createFailureBuilder(job)
                .retries(3)
                .retryTimeout(Duration.ofMinutes(3))
                .fail();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/fail");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          retries: 3,
                          retryTimeout: 'PT3M'
                        }
                        """);
    }

    @Test
    void failJobWithErrorMessageAndDetails() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        client.createFailureBuilder(job)
                .message("Test error")
                .details("Test error details")
                .fail();

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/fail");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          errorMessage: 'Test error',
                          errorDetails: 'Test error details'
                        }
                        """);
    }

    @Test
    void failJobWithException() {
        restInvoker.response = new TestRestResponse(204, "");
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        RuntimeException cause = new RuntimeException("Cause error");
        Exception failureException = new Exception("Test error", cause);
        client.createFailureBuilder(job)
                .error(failureException)
                .fail();

        String stackTrace = ExceptionUtils.readStackTrace(failureException)
                .replaceAll(System.lineSeparator(), "\\\\n")
                .replaceAll("\t", "\\\\t");

        assertThat(restInvoker.path).isEqualTo("/acquire/jobs/job-1/fail");
        assertThatJson(restInvoker.body)
                .isEqualTo("""
                        {
                          workerId: 'worker',
                          errorMessage: 'Test error',
                          errorDetails: '%s'
                        }
                        """.formatted(stackTrace));
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 400, 404, 500 })
    void failJobReturnsInvalidStatusCode(int statusCode) {
        String responseBody = "{ \"statusCode\": " + statusCode + " }";
        BaseAcquiredExternalWorkerJob job = new BaseAcquiredExternalWorkerJob();
        job.setId("job-1");
        job.setWorkerId("worker");
        restInvoker.response = new TestRestResponse(statusCode, responseBody);

        assertThatThrownBy(() -> client.createFailureBuilder(job).fail())
                .isInstanceOf(FlowableClientException.class)
                .hasMessage("Failing a job failed with status " + statusCode + " and body: " + responseBody);
    }

    @Test
    void acquireJobWithNullDueDate() {
        restInvoker.response = new TestRestResponse(200, """
                [
                  {
                    "id": "job-1",
                    "correlationId": "correlation-1",
                    "processInstanceId": "proc-1",
                    "executionId": "exec-1",
                    "processDefinitionId": "proc-def-1",
                    "tenantId": "flowable",
                    "elementId": "elem-1",
                    "elementName": "Element 1",
                    "createTime": "2023-08-21T13:12:28Z",
                    "dueDate": null,
                    "retries": 4,
                    "lockOwner": "tester-worker",
                    "lockExpirationTime": "2023-08-22T10:12:28Z"
                  }
                ]
                """);
        List<AcquiredExternalWorkerJob> jobs = client.createJobAcquireBuilder().topic("customer").acquireAndLock();

        assertThat(jobs).hasSize(1);
        AcquiredExternalWorkerJob job = jobs.get(0);
        assertThat(job.getId()).isEqualTo("job-1");
        assertThat(job.getCreateTime()).isEqualTo(Instant.parse("2023-08-21T13:12:28Z"));
        assertThat(job.getDueDate()).isNull();
        assertThat(job.getLockExpirationTime()).isEqualTo(Instant.parse("2023-08-22T10:12:28Z"));
    }

    static class TestRestInvoker implements RestInvoker {

        protected String path;
        protected ObjectNode body;
        protected RestResponse<String> response;

        @Override
        public RestResponse<String> post(String path, ObjectNode body) {
            this.path = path;
            this.body = body;
            return response;
        }
    }

    record TestRestResponse(int statusCode, String body) implements RestResponse<String> {

    }

}
