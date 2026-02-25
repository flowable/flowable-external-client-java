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

import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.impl.RestExternalWorkerClient;
import org.flowable.external.client.impl.RestInvoker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
public class DefaultTestConfiguration {

    @Bean
    public ExternalWorkerClient externalWorkerClient(RestInvoker restInvoker, ObjectMapper objectMapper) {
        return RestExternalWorkerClient.create("test-worker", restInvoker, objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestInvoker stubRestInvoker() {
        return new StubRestInvoker();
    }

}
