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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import org.flowable.external.client.impl.JavaHttpClientRestInvoker;
import org.flowable.external.client.impl.RestInvoker;

/**
 * @author Filip Hrisafov
 */
public class RestInvokerConfigurations {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(RestInvoker.class)
    @EnableConfigurationProperties(ExternalWorkerRestProperties.class)
    static class JavaHttpClientConfiguration {

        @Bean
        public RestInvoker flowableExternalWorkerRestInvoker(ExternalWorkerRestProperties properties) {
            String baseUrl = properties.determineExternalWorkerJobRestUrl();
            ExternalWorkerRestProperties.Authentication authentication = properties.getAuthentication();
            return switch (authentication.getType()) {
                case NONE -> JavaHttpClientRestInvoker.withoutAuthentication(baseUrl);
                case AUTO -> {
                    ExternalWorkerRestProperties.BearerAuthentication bearerAuthentication = authentication.getBearer();
                    ExternalWorkerRestProperties.BasicAuthentication basicAuthentication = authentication.getBasic();
                    String token = bearerAuthentication.getToken();
                    String username = basicAuthentication.getUsername();
                    String password = basicAuthentication.getPassword();
                    if (StringUtils.hasText(token) && (StringUtils.hasText(username) || StringUtils.hasText(password))) {
                        throw new IllegalArgumentException(
                                "Could not determine the authentication type either set flowable.external.worker.rest.authentication.bearer.token"
                                        + " or set flowable.external.worker.rest.authentication.basic.username and flowable.external.worker.rest.authentication.basic.password, but not both");
                    }
                    if (StringUtils.hasText(token)) {
                        yield JavaHttpClientRestInvoker.withAccessToken(baseUrl, token);
                    } else if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                        yield JavaHttpClientRestInvoker.withBasicAuth(baseUrl, username, password);
                    }

                    throw new IllegalArgumentException(
                            "Could not determine the authentication type either set flowable.external.worker.rest.authentication.bearer.token"
                                    + " or set flowable.external.worker.rest.authentication.basic.username and flowable.external.worker.rest.authentication.basic.password, but none were set");
                }
                case BASIC -> {
                    ExternalWorkerRestProperties.BasicAuthentication basicAuthentication = authentication.getBasic();
                    String username = basicAuthentication.getUsername();
                    Assert.hasText(username,
                            "flowable.external.worker.rest.authentication.basic.username has to be configured when using basic authentication");
                    String password = basicAuthentication.getPassword();
                    Assert.hasText(password,
                            "flowable.external.worker.rest.authentication.basic.password has to be configured when using basic authentication");
                    yield JavaHttpClientRestInvoker.withBasicAuth(baseUrl, username, password);
                }
                case BEARER -> {
                    ExternalWorkerRestProperties.BearerAuthentication bearerAuthentication = authentication.getBearer();
                    String token = bearerAuthentication.getToken();
                    Assert.hasText(token, "flowable.external.worker.rest.authentication.bearer.token has to be configured when using basic authentication");
                    yield JavaHttpClientRestInvoker.withAccessToken(baseUrl, token);
                }
            };
        }
    }

}
