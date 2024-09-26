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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.external.worker.rest")
public class ExternalWorkerRestProperties implements InitializingBean {

    /**
     * The base URL of the Flowable application.
     */
    private String baseUrl = "https://trial.flowable.com/work/";

    /**
     * The context path of the External Job REST API
     */
    private String contextPath = "/external-job-api";

    /**
     * The authentication configuration for the communication with Flowable.
     */
    @NestedConfigurationProperty
    private final Authentication authentication = new Authentication();

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(baseUrl, "flowable.external.worker.rest.base-url has to be configured");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String determineExternalWorkerJobRestUrl() {
        StringBuilder sb = new StringBuilder(baseUrl.length() + contextPath.length() + 1);
        sb.append(baseUrl);
        if (baseUrl.endsWith("/") && contextPath.startsWith("/")) {
            sb.append(contextPath.substring(1));
        } else if (!contextPath.startsWith("/")) {
            sb.append('/').append(contextPath);
        } else {
            sb.append(contextPath);
        }
        return sb.toString();
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * The type of authentication that should be used when communicating with Flowable.
     */
    public enum AuthenticationType {
        /**
         * It will be determined based on the set properties.
         * If multiple properties are set then an exception is thrown
         */
        AUTO,
        /**
         * No authentication should be used when communicating with Flowable.
         */
        NONE,
        /**
         * Use HTTP Basic username and password authentication.
         * If this is set then the username and password have to be provided.
         */
        BASIC,
        /**
         * Use HTTP Bearer token authentication.
         * If this is set then the access token has to be provided.
         */
        BEARER
    }

    /**
     * Properties for the REST authentication
     */
    public static class Authentication {

        /**
         * The type of authentication that should be used.
         */
        private AuthenticationType type = AuthenticationType.AUTO;

        /**
         * The basic authentication properties.
         */
        @NestedConfigurationProperty
        private final BasicAuthentication basic = new BasicAuthentication();

        /**
         * The bearer authentication properties.
         */
        @NestedConfigurationProperty
        private final BearerAuthentication bearer = new BearerAuthentication();

        public AuthenticationType getType() {
            return type;
        }

        public void setType(AuthenticationType type) {
            this.type = type;
        }

        public BasicAuthentication getBasic() {
            return basic;
        }

        public BearerAuthentication getBearer() {
            return bearer;
        }
    }

    /**
     * Properties for the basic authentication
     */
    public static class BasicAuthentication {

        /**
         * The username that should be used to authenticate with Flowable.
         */
        private String username;

        /**
         * The password that should be used to authenticate with Flowable.
         */
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Properties for the Bearer authentication
     */
    public static class BearerAuthentication {

        /**
         * The access token that should be used when authenticating with Flowable.
         */
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

}
