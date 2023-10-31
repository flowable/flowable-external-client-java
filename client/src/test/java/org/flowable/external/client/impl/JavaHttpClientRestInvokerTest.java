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
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.NoopLogger;
import org.microhttp.OptionsBuilder;
import org.microhttp.Request;
import org.microhttp.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
class JavaHttpClientRestInvokerTest {

    protected EventLoop eventLoop;
    protected TestMicrohttpHandler handler;
    protected String baseUrl;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        handler = new TestMicrohttpHandler();
        eventLoop = new EventLoop(OptionsBuilder.newBuilder().withPort(0).withConcurrency(1).build(), NoopLogger.instance(), handler);
        eventLoop.start();
        baseUrl = "http://localhost:" + eventLoop.getPort();
    }

    @AfterEach
    void tearDown() {
        if (eventLoop != null) {
            eventLoop.stop();
            try {
                eventLoop.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void withoutAuthentication() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withoutAuthentication(baseUrl);
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        RestResponse<String> response = restInvoker.post("/work/jobs", request);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThatJson(response.body()).isEqualTo("{}");

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json")
                );
        assertThat(receivedRequest.headers())
                .extracting(Header::name)
                .doesNotContain("Authorization");
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @Test
    void withoutAuthenticationWithSlashInBaseUrl() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withoutAuthentication(baseUrl + "/");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        restInvoker.post("/work/jobs", request);

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json")
                );
        assertThat(receivedRequest.headers())
                .extracting(Header::name)
                .doesNotContain("Authorization");
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @Test
    void withoutAuthenticationWithContextPathInBaseUrl() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withoutAuthentication(baseUrl + "/work");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        restInvoker.post("/jobs", request);

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json")
                );
        assertThat(receivedRequest.headers())
                .extracting(Header::name)
                .doesNotContain("Authorization");
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @Test
    void withBasicAuthentication() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withBasicAuth(baseUrl + "/work", "admin", "test");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        restInvoker.post("/jobs", request);

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json"),
                        tuple("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:test".getBytes(StandardCharsets.ISO_8859_1)))
                );
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @Test
    void withAccessTokenAuthentication() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withAccessToken(baseUrl + "/work", "SomeToken");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        restInvoker.post("/jobs", request);

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json"),
                        tuple("Authorization", "Bearer SomeToken")
                );
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @Test
    void withAccessTokenSupplierAuthentication() {
        handler.addJsonResponse(200, "{}");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withAccessToken(baseUrl + "/work", () -> "SomeToken");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        restInvoker.post("/jobs", request);

        Request receivedRequest = handler.takeRequest();
        assertThat(receivedRequest.method()).isEqualTo("POST");
        assertThat(receivedRequest.uri()).isEqualTo("/work/jobs");
        assertThat(receivedRequest.headers())
                .extracting(Header::name, Header::value)
                .contains(
                        tuple("Content-Type", "application/json"),
                        tuple("Authorization", "Bearer SomeToken")
                );
        assertThatJson(new String(receivedRequest.body(), StandardCharsets.UTF_8))
                .isEqualTo("{ workerId: 'test' }");
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 201, 400, 404, 505 })
    void responseWithDifferentStatusCodes(int statusCode) {
        handler.addJsonResponse(statusCode, "{ \"status\": " + statusCode + " }");
        RestInvoker restInvoker = JavaHttpClientRestInvoker.withoutAuthentication(baseUrl + "/work");
        ObjectNode request = objectMapper.createObjectNode()
                .put("workerId", "test");
        RestResponse<String> response = restInvoker.post("/jobs", request);
        assertThat(response.statusCode()).isEqualTo(statusCode);
        assertThatJson(response.body()).isEqualTo("{ status: " + statusCode + " }");
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

        protected void addJsonResponse(int status, String body) {
            responseQueue.add(new Response(status, String.valueOf(status), List.of(new Header("Content-Type", "application/json")),
                    body.getBytes(StandardCharsets.UTF_8)));
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
