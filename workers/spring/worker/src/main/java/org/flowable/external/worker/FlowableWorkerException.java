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

import java.time.Duration;

/**
 * @author Filip Hrisafov
 */
public class FlowableWorkerException extends RuntimeException {

    protected String errorDetails;
    protected int retries;
    protected Duration retryTimeout;

    public FlowableWorkerException(String message) {
        super(message);
    }

    public FlowableWorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public Duration getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(Duration retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
}
