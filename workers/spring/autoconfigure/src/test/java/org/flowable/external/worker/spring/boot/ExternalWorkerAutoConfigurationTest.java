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
package org.flowable.external.worker.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.NoopLogger;
import org.microhttp.OptionsBuilder;
import org.microhttp.Request;
import org.microhttp.Response;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.impl.JavaHttpClientRestInvoker;
import org.flowable.external.client.impl.RestInvoker;
import org.flowable.external.worker.config.DefaultFlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerEndpoint;
import org.flowable.external.worker.listener.WorkerJobListenerContainer;

/**
 * @author Filip Hrisafov
 */
class ExternalWorkerAutoConfigurationTest {

    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExternalWorkerAutoConfiguration.class));

    @Test
    void defaultConfiguration() {
        contextRunner
                .withPropertyValues("flowable.external.worker.rest.authentication.type=none")
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(ExternalWorkerClient.class)
                            .hasSingleBean(RestInvoker.class)
                            .hasSingleBean(FlowableWorkerContainerFactory.class);

                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    assertThat(restInvoker).isInstanceOf(JavaHttpClientRestInvoker.class);

                    WorkerJobListenerContainer container = getContainer(context, "flowableWorkerContainerFactory");
                    assertThat(container.getConcurrency()).isEqualTo(1);
                    assertThat(container.getLockDuration()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(container.getNumberOfRetries()).isEqualTo(5);
                    assertThat(container.getNumberOfTasks()).isEqualTo(1);
                    assertThat(container.getPollingInterval()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void customConfiguration() {
        contextRunner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=none",
                        "flowable.external.worker.concurrency=2",
                        "flowable.external.worker.lock-duration=10m",
                        "flowable.external.worker.number-of-retries=3",
                        "flowable.external.worker.number-of-tasks=2",
                        "flowable.external.worker.polling-interval=10s"
                )
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(ExternalWorkerClient.class)
                            .hasSingleBean(RestInvoker.class)
                            .hasSingleBean(FlowableWorkerContainerFactory.class);

                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    assertThat(restInvoker).isInstanceOf(JavaHttpClientRestInvoker.class);

                    WorkerJobListenerContainer container = getContainer(context, "flowableWorkerContainerFactory");
                    assertThat(container.getConcurrency()).isEqualTo(2);
                    assertThat(container.getLockDuration()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(container.getNumberOfRetries()).isEqualTo(3);
                    assertThat(container.getNumberOfTasks()).isEqualTo(2);
                    assertThat(container.getPollingInterval()).isEqualTo(Duration.ofSeconds(10));
                });
    }

    @Test
    void defaultConfigurationForRestInvoker() {
        runInEventLoop((runner, handler) -> runner.run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Could not determine the authentication type either set"
                        + " flowable.external.worker.rest.authentication.bearer.token or set"
                        + " flowable.external.worker.rest.authentication.basic.username and flowable.external.worker.rest.authentication.basic.password,"
                        + " but none were set")));
    }

    @Test
    void autoAuthenticationWithBearerAndBasicUserSet() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.bearer.token=token",
                        "flowable.external.worker.rest.authentication.basic.username=admin"
                )
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Could not determine the authentication type either set"
                                + " flowable.external.worker.rest.authentication.bearer.token or set"
                                + " flowable.external.worker.rest.authentication.basic.username and flowable.external.worker.rest.authentication.basic.password,"
                                + " but not both")));
    }

    @Test
    void autoAuthenticationWithBearerAndBasicPasswordSet() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.bearer.token=token",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Could not determine the authentication type either set"
                                + " flowable.external.worker.rest.authentication.bearer.token or set"
                                + " flowable.external.worker.rest.authentication.basic.username and flowable.external.worker.rest.authentication.basic.password,"
                                + " but not both")));
    }

    @Test
    void autoAuthenticationWithBearerOnly() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.bearer.token=SomeToken"
                )
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    assertThat(context).hasSingleBean(RestInvoker.class);
                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    restInvoker.post("/test", objectMapper.createObjectNode());
                    Request request = handler.takeRequest();
                    assertThat(request.uri()).isEqualTo("/external-job-api/test");
                    assertThat(request.headers())
                            .extracting(Header::name, Header::value)
                            .contains(
                                    tuple("Content-Type", "application/json"),
                                    tuple("Authorization", "Bearer SomeToken")
                            );
                }));
    }

    @Test
    void autoAuthenticationWithBasicOnly() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.basic.username=admin",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    assertThat(context).hasSingleBean(RestInvoker.class);
                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    restInvoker.post("/test", objectMapper.createObjectNode());
                    Request request = handler.takeRequest();
                    assertThat(request.uri()).isEqualTo("/external-job-api/test");
                    assertThat(request.headers())
                            .extracting(Header::name, Header::value)
                            .contains(
                                    tuple("Content-Type", "application/json"),
                                    tuple("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:test".getBytes(StandardCharsets.ISO_8859_1)))
                            );
                }));
    }

    @Test
    void basicAuthenticationWithoutBasic() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=basic",
                        "flowable.external.worker.rest.authentication.bearer.token=dummyToken"
                )
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("flowable.external.worker.rest.authentication.basic.username has to be configured when using basic authentication")));
    }

    @Test
    void basicAuthenticationWithoutBasicPassword() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=basic",
                        "flowable.external.worker.rest.authentication.bearer.token=dummyToken",
                        "flowable.external.worker.rest.authentication.basic.username=admin"
                )
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("flowable.external.worker.rest.authentication.basic.password has to be configured when using basic authentication")));
    }

    @Test
    void basicAuthenticationWithCorrectConfiguration() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=basic",
                        "flowable.external.worker.rest.authentication.bearer.token=dummyToken",
                        "flowable.external.worker.rest.authentication.basic.username=admin",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    assertThat(context).hasSingleBean(RestInvoker.class);
                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    restInvoker.post("/test", objectMapper.createObjectNode());
                    Request request = handler.takeRequest();
                    assertThat(request.uri()).isEqualTo("/external-job-api/test");
                    assertThat(request.headers())
                            .extracting(Header::name, Header::value)
                            .contains(
                                    tuple("Content-Type", "application/json"),
                                    tuple("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:test".getBytes(StandardCharsets.ISO_8859_1)))
                            );
                }));
    }

    @Test
    void bearerAuthenticationWithoutToken() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=bearer",
                        "flowable.external.worker.rest.authentication.basic.username=admin",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("flowable.external.worker.rest.authentication.bearer.token has to be configured when using basic authentication")));
    }

    @Test
    void bearerAuthenticationWithToken() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=bearer",
                        "flowable.external.worker.rest.authentication.bearer.token=SomeToken",
                        "flowable.external.worker.rest.authentication.basic.username=admin",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    assertThat(context).hasSingleBean(RestInvoker.class);
                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    restInvoker.post("/test", objectMapper.createObjectNode());
                    Request request = handler.takeRequest();
                    assertThat(request.uri()).isEqualTo("/external-job-api/test");
                    assertThat(request.headers())
                            .extracting(Header::name, Header::value)
                            .contains(
                                    tuple("Content-Type", "application/json"),
                                    tuple("Authorization", "Bearer SomeToken")
                            );
                }));
    }

    @Test
    void noneAuthentication() {
        runInEventLoop((runner, handler) -> runner
                .withPropertyValues(
                        "flowable.external.worker.rest.authentication.type=none",
                        "flowable.external.worker.rest.authentication.bearer.token=SomeToken",
                        "flowable.external.worker.rest.authentication.basic.username=admin",
                        "flowable.external.worker.rest.authentication.basic.password=test"
                )
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    assertThat(context).hasSingleBean(RestInvoker.class);
                    RestInvoker restInvoker = context.getBean(RestInvoker.class);
                    restInvoker.post("/test", objectMapper.createObjectNode());
                    Request request = handler.takeRequest();
                    assertThat(request.uri()).isEqualTo("/external-job-api/test");
                    assertThat(request.headers())
                            .extracting(Header::name, Header::value)
                            .contains(tuple("Content-Type", "application/json"));
                    assertThat(request.headers())
                            .extracting(Header::name)
                            .doesNotContain("Authorization");
                }));
    }

    protected void runInEventLoop(BiConsumer<ApplicationContextRunner, TestMicrohttpHandler> runner) {
        EventLoop eventLoop = null;
        try {
            TestMicrohttpHandler handler = new TestMicrohttpHandler();
            eventLoop = new EventLoop(OptionsBuilder.newBuilder().withPort(0).withConcurrency(1).build(), NoopLogger.instance(), handler);
            eventLoop.start();
            String baseUrl = "http://localhost:" + eventLoop.getPort();
            runner.accept(contextRunner.withPropertyValues("flowable.external.worker.rest.base-url=" + baseUrl), handler);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (eventLoop != null) {
                eventLoop.stop();
                try {
                    eventLoop.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    protected WorkerJobListenerContainer getContainer(AssertableApplicationContext loaded, String name) {
        FlowableWorkerContainerFactory<?> factory = loaded.getBean(name, FlowableWorkerContainerFactory.class);
        assertThat(factory).isInstanceOf(DefaultFlowableWorkerContainerFactory.class);
        return ((DefaultFlowableWorkerContainerFactory) factory).createWorkerContainer(mock(FlowableWorkerEndpoint.class));
    }

    static class TestMicrohttpHandler implements Handler {

        protected final BlockingQueue<Request> requestQueue = new LinkedBlockingDeque<>();
        protected final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
        protected final Response defaultResponse;

        TestMicrohttpHandler() {
            this(new Response(501, "Not Implemented", List.of(new Header("Content-Type", "application/json")), "{}".getBytes(StandardCharsets.UTF_8)));
        }

        TestMicrohttpHandler(Response defaultResponse) {
            this.defaultResponse = defaultResponse;
        }

        @Override
        public void handle(Request request, Consumer<Response> callback) {
            if (responseQueue.isEmpty()) {
                callback.accept(defaultResponse);
            } else {
                try {
                    Response response = responseQueue.take();
                    callback.accept(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            requestQueue.add(request);
        }

        protected Request takeRequest() {
            try {
                return requestQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
