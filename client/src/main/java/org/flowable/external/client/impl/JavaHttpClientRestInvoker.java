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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import java.util.function.Supplier;

import org.flowable.external.client.FlowableClientException;

import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class JavaHttpClientRestInvoker implements RestInvoker {

    protected final Function<String, HttpRequest.Builder> requestBuilderCreator;
    protected final HttpClient httpClient;

    public JavaHttpClientRestInvoker(Function<String, HttpRequest.Builder> requestBuilderCreator, HttpClient httpClient) {
        this.requestBuilderCreator = requestBuilderCreator;
        this.httpClient = httpClient;
    }

    @Override
    public RestResponse<String> post(String path, ObjectNode requestBody) {
        HttpRequest request = requestBuilderCreator.apply(path)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new FlowableClientException("Failed to POST to " + path, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread has been interrupted", ex);
        }
        return new HttpReponseRestResponse(response);
    }

    public static JavaHttpClientRestInvoker withBasicAuth(String baseUrl, String username, String password) {
        return withAuthorizationHeader(baseUrl,
                () -> "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1)));
    }

    public static JavaHttpClientRestInvoker withAccessToken(String baseUrl, String accessToken) {
        return withAccessToken(baseUrl, () -> accessToken);
    }

    public static JavaHttpClientRestInvoker withAccessToken(String baseUrl, Supplier<String> accessToken) {
        return withAuthorizationHeader(baseUrl, () -> "Bearer " + accessToken.get());
    }

    public static JavaHttpClientRestInvoker withoutAuthentication(String baseUrl) {
        return withAuthorizationHeader(baseUrl, null);
    }

    protected static JavaHttpClientRestInvoker withAuthorizationHeader(String baseUrl, Supplier<String> authorizationHeaderProvider) {
        FlowableRequestBuilderProvider requestBuilderProvider = new FlowableRequestBuilderProvider(baseUrl, authorizationHeaderProvider);
        HttpClient httpClient = HttpClient.newBuilder().build();
        return new JavaHttpClientRestInvoker(requestBuilderProvider, httpClient);
    }

    protected static class HttpReponseRestResponse implements RestResponse<String> {

        protected final HttpResponse<String> response;

        protected HttpReponseRestResponse(HttpResponse<String> response) {
            this.response = response;
        }

        @Override
        public String body() {
            return response.body();
        }

        @Override
        public int statusCode() {
            return response.statusCode();
        }
    }

    protected static class FlowableRequestBuilderProvider implements Function<String, HttpRequest.Builder> {

        protected final String baseUrl;
        protected final Supplier<String> authorizationHeaderProvider;

        protected FlowableRequestBuilderProvider(String baseUrl, Supplier<String> authorizationHeaderProvider) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : (baseUrl);
            this.authorizationHeaderProvider = authorizationHeaderProvider;
        }

        @Override
        public HttpRequest.Builder apply(String path) {
            String pathWithPrefix = path.startsWith("/") ? path : ("/" + path);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + pathWithPrefix));
            if (authorizationHeaderProvider != null) {
                builder = builder.header("Authorization", authorizationHeaderProvider.get());
            }
            return builder;
        }
    }
}
