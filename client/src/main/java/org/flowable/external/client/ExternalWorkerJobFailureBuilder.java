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
package org.flowable.external.client;

import java.time.Duration;

/**
 * A builder that can be used to complete a specific job.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobFailureBuilder {

    /**
     * Provide fail information based on the exception.
     * The {@link Exception#getMessage()} will be used as the failure message
     * and the exception stacktrace will be used as the details.
     *
     * @param exception the exception that should be used to provide the message and details
     * @see #message(String)
     * @see #details(String)
     */
    ExternalWorkerJobFailureBuilder error(Exception exception);

    /**
     * The failure message that should be passed to the Flowable application.
     *
     * @param message the failure message
     */
    ExternalWorkerJobFailureBuilder message(String message);

    /**
     * The failure details that should be passed to the Flowable application.
     *
     * @param details the failure details
     */
    ExternalWorkerJobFailureBuilder details(String details);

    /**
     * How many times should the job be retried.
     * This can be used to increase / decrease the existing retries of a job.
     * When nothing is set then the Flowable application is going to decrease the retries by 1.
     *
     * @param retries the new retry count for the job
     */
    ExternalWorkerJobFailureBuilder retries(int retries);

    /**
     * How long should the Flowable application wait before making this job available for execution.
     * This can be used to set a specific period after which the execution of a job would be successful.
     *
     * @param retryTimeout the period that the Flowable application should wait before making this job available
     */
    ExternalWorkerJobFailureBuilder retryTimeout(Duration retryTimeout);

    /**
     * Execute the failure of the job.
     */
    void fail();

}
