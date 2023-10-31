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
package org.flowable.external.worker.annotation;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.ExternalWorkerJob;
import org.flowable.external.worker.DefaultTestConfiguration;
import org.flowable.external.worker.SimpleWorker;
import org.flowable.external.worker.SimpleWorkerExceptionThrowing;
import org.flowable.external.worker.SimpleWorkerReturnsWorkerResult;
import org.flowable.external.worker.SimpleWorkerWithAnnotationFixValues;
import org.flowable.external.worker.SimpleWorkerWithProperties;
import org.flowable.external.worker.StubRestInvoker;
import org.flowable.external.worker.config.DefaultFlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
class FlowableWorkerTest {

    @Test
    void defaultContainerFactory() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("defaultContainerFactory");
            context.register(DefaultContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorker.class);
            context.refresh();
            context.start();
            SimpleWorker simpleWorker = context.getBean(SimpleWorker.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(12))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simple',
                                      lockDuration: 'PT5M',
                                      numberOfRetries: 5,
                                      numberOfTasks: 1
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(2)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0))
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-2/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(1));

        }
    }

    @Test
    void containerFactoryWithSetDefaults() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("containerFactoryWithSetDefaults");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorker.class);
            context.refresh();
            context.start();
            SimpleWorker simpleWorker = context.getBean(SimpleWorker.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simple',
                                      lockDuration: 'PT1M',
                                      numberOfRetries: 4,
                                      numberOfTasks: 2
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(2)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0))
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-2/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(1));

        }
    }

    @Test
    void flowableWorkerWithSetValuesInAnnotation() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("flowableWorkerWithSetValuesInAnnotation");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerWithAnnotationFixValues.class);
            context.refresh();
            context.start();
            SimpleWorkerWithAnnotationFixValues simpleWorker = context.getBean(SimpleWorkerWithAnnotationFixValues.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(250))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simpleWithProperties',
                                      lockDuration: 'PT10M',
                                      numberOfRetries: 1,
                                      numberOfTasks: 3
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(2)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0))
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-2/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(1));

        }
    }

    @Test
    void flowableWorkerWithPropertiesInAnnotation() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("flowableWorkerWithPropertiesInAnnotation");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerWithProperties.class);
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test-properties", Map.of(
                            "topic", "customer",
                            "lockDuration", "PT7M",
                            "numberOfTasks", "10",
                            "numberOfRetries", "6",
                            "pollingInterval", "PT0.2S"
                    )));
            context.refresh();
            context.start();
            SimpleWorkerWithProperties simpleWorker = context.getBean(SimpleWorkerWithProperties.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(250))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'customer',
                                      lockDuration: 'PT7M',
                                      numberOfRetries: 6,
                                      numberOfTasks: 10
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0));

        }
    }

    @Test
    void workerThrowsException() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerThrowsException");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerExceptionThrowing.class);
            context.refresh();
            context.start();
            SimpleWorkerExceptionThrowing simpleWorker = context.getBean(SimpleWorkerExceptionThrowing.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                        [{
                            "id": "job-1", "lockOwner": "test-worker",
                            "variables": [
                              { "name": "exceptionMessage", "type": "string", "value": "Test error" }
                            ]
                        }]
                    """);
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simpleWithException',
                                      lockDuration: 'PT1M',
                                      numberOfRetries: 4,
                                      numberOfTasks: 2
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-2/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0));

            assertThat(restInvoker.getFailJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/fail");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  errorMessage: '${json-unit.any-string}',
                                                  errorDetails: '${json-unit.any-string}'
                                                }
                                                """);
                                assertThat(request.body().path("errorMessage").asText())
                                        .startsWith("Listener method");
                                assertThat(request.body().path("errorDetails").asText())
                                        .contains("Test error");
                            },
                            atIndex(0));

        }
    }

    @Test
    void workerThrowsWorkerExceptionException() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerThrowsWorkerExceptionException");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerExceptionThrowing.class);
            context.refresh();
            context.start();
            SimpleWorkerExceptionThrowing simpleWorker = context.getBean(SimpleWorkerExceptionThrowing.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                        [
                          {
                            "id": "job-1", "lockOwner": "test-worker",
                            "variables": [
                              {
                                "name": "workerException", "type": "json",
                                "value": {
                                  "message": "Test error",
                                  "details": "Test error details",
                                  "retries": 2,
                                  "retryTimeout": "PT2M"
                                }
                              }
                            ]
                          }
                        ]
                    """);
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simpleWithException',
                                      lockDuration: 'PT1M',
                                      numberOfRetries: 4,
                                      numberOfTasks: 2
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-2/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker'
                                                }
                                                """);
                            },
                            atIndex(0));

            assertThat(restInvoker.getFailJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/fail");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  errorMessage: 'Test error',
                                                  errorDetails: 'Test error details',
                                                  retries: 2,
                                                  retryTimeout: 'PT2M'
                                                }
                                                """);
                            },
                            atIndex(0));

        }
    }

    @Test
    void workerReturnsFailureResult() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerReturnsFailureResult");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerReturnsWorkerResult.class);
            context.refresh();
            context.start();
            SimpleWorkerReturnsWorkerResult simpleWorker = context.getBean(SimpleWorkerReturnsWorkerResult.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                        [
                          {
                            "id": "job-1", "lockOwner": "test-worker",
                            "variables": [
                              {
                                "name": "workerFailure", "type": "json",
                                "value": {
                                  "message": "Test error",
                                  "details": "Test error details",
                                  "retries": 2,
                                  "retryTimeout": "PT2M"
                                }
                              }
                            ]
                          }
                        ]
                    """);

            await()
                    .atMost(Duration.ofSeconds(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simpleWithReturnResult',
                                      lockDuration: 'PT1M',
                                      numberOfRetries: 4,
                                      numberOfTasks: 2
                                    }
                                    """));

            assertThat(restInvoker.getFailJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/fail");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  errorMessage: 'Test error',
                                                  errorDetails: 'Test error details',
                                                  retries: 2,
                                                  retryTimeout: 'PT2M'
                                                }
                                                """);
                            },
                            atIndex(0));

        }
    }

    @Test
    void workerReturnsSuccessResult() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerReturnsSuccessResult");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerReturnsWorkerResult.class);
            context.refresh();
            context.start();
            SimpleWorkerReturnsWorkerResult simpleWorker = context.getBean(SimpleWorkerReturnsWorkerResult.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");

            await()
                    .atMost(Duration.ofHours(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'simpleWithReturnResult',
                                      lockDuration: 'PT1M',
                                      numberOfRetries: 4,
                                      numberOfTasks: 2
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/complete");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  variables: [
                                                    { name: 'stringVar', type: 'string', value: 'String value' },
                                                    { name: 'shortVar', type: 'short', value: 12 },
                                                    { name: 'integerVar', type: 'integer', value: 42 },
                                                    { name: 'longVar', type: 'long', value: 4242 },
                                                    { name: 'doubleVar', type: 'double', value: 42.42 },
                                                    { name: 'booleanVar', type: 'boolean', value: true },
                                                    { name: 'dateVar', type: 'date', value: '2023-08-23T08:45:12Z' },
                                                    { name: 'instantVar', type: 'instant', value: '2023-08-23T10:15:27Z' },
                                                    { name: 'localDateVar', type: 'localDate', value: '2023-08-22' },
                                                    { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-22T08:34:12' },
                                                    { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                                                    { name: 'jsonArrayVar', type: 'json', value: [1, 2] },
                                                    { name: 'jsonMapVar', type: 'json', value: { name: 'Fozzie', age: 25 } },
                                                    { name: 'jsonListVar', type: 'json', value: ['Kermit', 'Fozzie'] }
                                                  ]
                                                }
                                                """);
                            },
                            atIndex(0));
        }
    }

    @Test
    void workerReturnsBpmnErrorResult() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerReturnsBpmnErrorResult");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerReturnsWorkerResult.class);
            context.refresh();
            context.start();
            SimpleWorkerReturnsWorkerResult simpleWorker = context.getBean(SimpleWorkerReturnsWorkerResult.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                    [
                      {
                        "id": "job-1",
                        "scopeType": "bpmn",
                        "lockOwner": "test-worker",
                        "variables": [
                            { "name": "successType", "type": "string", "value": "bpmnError" }
                        ]
                      }
                    ]
                    """);

            await()
                    .atMost(Duration.ofHours(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getBpmnErrorRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/bpmnError");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  variables: [
                                                    { name: 'stringVar', type: 'string', value: 'String value' },
                                                    { name: 'shortVar', type: 'short', value: 12 },
                                                    { name: 'integerVar', type: 'integer', value: 42 },
                                                    { name: 'longVar', type: 'long', value: 4242 },
                                                    { name: 'doubleVar', type: 'double', value: 42.42 },
                                                    { name: 'booleanVar', type: 'boolean', value: true },
                                                    { name: 'dateVar', type: 'date', value: '2023-08-23T08:45:12Z' },
                                                    { name: 'instantVar', type: 'instant', value: '2023-08-23T10:15:27Z' },
                                                    { name: 'localDateVar', type: 'localDate', value: '2023-08-22' },
                                                    { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-22T08:34:12' },
                                                    { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                                                    { name: 'jsonArrayVar', type: 'json', value: [1, 2] },
                                                    { name: 'jsonMapVar', type: 'json', value: { name: 'Fozzie', age: 25 } },
                                                    { name: 'jsonListVar', type: 'json', value: ['Kermit', 'Fozzie'] }
                                                  ]
                                                }
                                                """);
                            },
                            atIndex(0));
        }
    }

    @Test
    void workerReturnsBpmnErrorWithErrorCodeResult() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerReturnsBpmnErrorWithErrorCodeResult");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerReturnsWorkerResult.class);
            context.refresh();
            context.start();
            SimpleWorkerReturnsWorkerResult simpleWorker = context.getBean(SimpleWorkerReturnsWorkerResult.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                    [
                      {
                        "id": "job-1",
                        "scopeType": "bpmn",
                        "lockOwner": "test-worker",
                        "variables": [
                            { "name": "successType", "type": "string", "value": "bpmnError" },
                            { "name": "errorCode", "type": "string", "value": "testError" }
                        ]
                      }
                    ]
                    """);

            await()
                    .atMost(Duration.ofHours(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getBpmnErrorRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/bpmnError");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  variables: [
                                                    { name: 'stringVar', type: 'string', value: 'String value' },
                                                    { name: 'shortVar', type: 'short', value: 12 },
                                                    { name: 'integerVar', type: 'integer', value: 42 },
                                                    { name: 'longVar', type: 'long', value: 4242 },
                                                    { name: 'doubleVar', type: 'double', value: 42.42 },
                                                    { name: 'booleanVar', type: 'boolean', value: true },
                                                    { name: 'dateVar', type: 'date', value: '2023-08-23T08:45:12Z' },
                                                    { name: 'instantVar', type: 'instant', value: '2023-08-23T10:15:27Z' },
                                                    { name: 'localDateVar', type: 'localDate', value: '2023-08-22' },
                                                    { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-22T08:34:12' },
                                                    { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                                                    { name: 'jsonArrayVar', type: 'json', value: [1, 2] },
                                                    { name: 'jsonMapVar', type: 'json', value: { name: 'Fozzie', age: 25 } },
                                                    { name: 'jsonListVar', type: 'json', value: ['Kermit', 'Fozzie'] }
                                                  ],
                                                  errorCode: 'testError'
                                                }
                                                """);
                            },
                            atIndex(0));
        }
    }

    @Test
    void workerReturnsCmmnTerminateResult() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("workerReturnsCmmnTerminateResult");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, SimpleWorkerReturnsWorkerResult.class);
            context.refresh();
            context.start();
            SimpleWorkerReturnsWorkerResult simpleWorker = context.getBean(SimpleWorkerReturnsWorkerResult.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireResponse(200, """
                    [
                      {
                        "id": "job-1",
                        "scopeType": "cmmn",
                        "lockOwner": "test-worker",
                        "variables": [
                            { "name": "successType", "type": "string", "value": "cmmnTerminate" }
                        ]
                      }
                    ]
                    """);

            await()
                    .atMost(Duration.ofHours(3))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(simpleWorker.getReceivedJobs()).hasSize(1));

            context.close();

            assertThat(simpleWorker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactly("job-1");

            assertThat(restInvoker.getCmmnTerminateRequests())
                    .hasSize(1)
                    .satisfies(request -> {
                                assertThat(request.path()).isEqualTo("/acquire/jobs/job-1/cmmnTerminate");
                                assertThatJson(request.body())
                                        .isEqualTo("""
                                                {
                                                  workerId: 'test-worker',
                                                  variables: [
                                                    { name: 'stringVar', type: 'string', value: 'String value' },
                                                    { name: 'shortVar', type: 'short', value: 12 },
                                                    { name: 'integerVar', type: 'integer', value: 42 },
                                                    { name: 'longVar', type: 'long', value: 4242 },
                                                    { name: 'doubleVar', type: 'double', value: 42.42 },
                                                    { name: 'booleanVar', type: 'boolean', value: true },
                                                    { name: 'dateVar', type: 'date', value: '2023-08-23T08:45:12Z' },
                                                    { name: 'instantVar', type: 'instant', value: '2023-08-23T10:15:27Z' },
                                                    { name: 'localDateVar', type: 'localDate', value: '2023-08-22' },
                                                    { name: 'localDateTimeVar', type: 'localDateTime', value: '2023-08-22T08:34:12' },
                                                    { name: 'jsonObjectVar', type: 'json', value: { name: 'Kermit' } },
                                                    { name: 'jsonArrayVar', type: 'json', value: [1, 2] },
                                                    { name: 'jsonMapVar', type: 'json', value: { name: 'Fozzie', age: 25 } },
                                                    { name: 'jsonListVar', type: 'json', value: ['Kermit', 'Fozzie'] }
                                                  ]
                                                }
                                                """);
                            },
                            atIndex(0));
        }
    }

    @Test
    void concurrentWorkers() {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setDisplayName("concurrentWorkers");
            context.register(CustomizedContainerFactoryConfiguration.class, DefaultTestConfiguration.class, ConcurrentWorker.class);
            context.refresh();
            context.start();
            ConcurrentWorker worker = context.getBean(ConcurrentWorker.class);
            StubRestInvoker restInvoker = context.getBean(StubRestInvoker.class);
            restInvoker.addAcquireJob("job-1", "test-worker");
            restInvoker.addAcquireJob("job-2", "test-worker");

            await()
                    .atMost(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> assertThat(worker.getReceivedJobs()).hasSize(2));

            context.close();

            assertThat(worker.getReceivedJobs())
                    .extracting(ExternalWorkerJob::getId)
                    .containsExactlyInAnyOrder("job-1", "job-2");

            assertThat(restInvoker.getAcquireJobsRequests())
                    .allSatisfy(request -> assertThatJson(request.body())
                            .when(Option.IGNORING_EXTRA_FIELDS)
                            .isEqualTo("""
                                    {
                                      workerId: 'test-worker',
                                      topic: 'concurrentTopic'
                                    }
                                    """));

            assertThat(restInvoker.getCompleteJobRequests())
                    .hasSize(2);

        }
    }

    @Configuration
    @EnableFlowableWorker
    static class DefaultContainerFactoryConfiguration {

        @Bean
        public FlowableWorkerContainerFactory<?> flowableWorkerContainerFactory(ExternalWorkerClient client) {
            DefaultFlowableWorkerContainerFactory factory = new DefaultFlowableWorkerContainerFactory();
            factory.setExternalWorkerClient(client);
            factory.setPollingInterval(Duration.ofSeconds(10));
            return factory;
        }
    }

    @Configuration
    @EnableFlowableWorker
    static class CustomizedContainerFactoryConfiguration {

        @Bean
        public FlowableWorkerContainerFactory<?> flowableWorkerContainerFactory(ExternalWorkerClient client) {
            DefaultFlowableWorkerContainerFactory factory = new DefaultFlowableWorkerContainerFactory();
            factory.setExternalWorkerClient(client);
            factory.setPollingInterval(Duration.ofSeconds(1));
            factory.setNumberOfTasks(2);
            factory.setNumberOfRetries(4);
            factory.setLockDuration(Duration.ofMinutes(1));
            return factory;
        }
    }

    @Component
    static class ConcurrentWorker {

        protected final CountDownLatch countDownLatch = new CountDownLatch(2);
        protected final List<AcquiredExternalWorkerJob> receivedJobs = new CopyOnWriteArrayList<>();

        @FlowableWorker(topic = "concurrentTopic", concurrency = "2", pollingInterval = "PT0.2S")
        public void processJob(AcquiredExternalWorkerJob job) {
            countDownLatch.countDown();
            try {
                if (countDownLatch.await(1, TimeUnit.SECONDS)) {
                    receivedJobs.add(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

        }

        public List<AcquiredExternalWorkerJob> getReceivedJobs() {
            return receivedJobs;
        }
    }
}
