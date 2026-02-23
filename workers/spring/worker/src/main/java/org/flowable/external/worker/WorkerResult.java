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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.flowable.external.worker.annotation.FlowableWorker;

import tools.jackson.databind.JsonNode;

/**
 * A result that can be returned by a method annotated with {@link FlowableWorker @FlowableWorker}.
 * A result can be created by using {@link WorkerResultBuilder} and defining it as a parameter in your annotated method.
 *
 * @author Filip Hrisafov
 * @see WorkerResultBuilder
 */
public sealed interface WorkerResult permits WorkerResult.Failure, WorkerResult.Success {

    /**
     * A failure result.
     * It allows for configuring the failure details.
     * Can be created via {@link WorkerResultBuilder#failure()}
     */
    non-sealed interface Failure extends WorkerResult {

        /**
         * Provide fail information based on the exception.
         * The {@link Exception#getMessage()} will be used as the failure message
         * and the exception stacktrace will be used as the details.
         *
         * @param exception the exception that should be used to provide the message and details
         * @see #message(String)
         * @see #details(String)
         */
        Failure error(Exception exception);

        /**
         * The failure message that should be passed to the Flowable application.
         *
         * @param message the failure message
         */
        Failure message(String message);

        /**
         * The failure details that should be passed to the Flowable application.
         *
         * @param details the failure details
         */
        Failure details(String details);

        /**
         * How many times should the job be retried.
         * This can be used to increase / decrease the existing retries of a job.
         * When nothing is set then the Flowable application is going to decrease the retries by 1.
         *
         * @param retries the new retry count for the job
         */
        Failure retries(int retries);

        /**
         * How long should the Flowable application wait before making this job available for execution.
         * This can be used to set a specific period after which the execution of a job would be successful.
         *
         * @param retryTimeout the period that the Flowable application should wait before making this job available
         */
        Failure retryTimeout(Duration retryTimeout);
    }

    /**
     * A successful result.
     * It allows for configuring the completion details.
     * Can be created via {@link WorkerResultBuilder#success()}
     */
    non-sealed interface Success extends WorkerResult {

        /**
         * Provide a {@link String} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, String value);

        /**
         * Provide a {@link Short} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Short value);

        /**
         * Provide an {@link Integer} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Integer value);

        /**
         * Provide a {@link Long} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Long value);

        /**
         * Provide a {@link Double} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Double value);

        /**
         * Provide a {@link Boolean} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Boolean value);

        /**
         * Provide a {@link Date} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Date value);

        /**
         * Provide an {@link Instant} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, Instant value);

        /**
         * Provide a {@link LocalDate} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, LocalDate value);

        /**
         * Provide a {@link LocalDateTime} variable that would be passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, LocalDateTime value);

        /**
         * Provide a {@link JsonNode} that would be passed as a json variable to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success variable(String name, JsonNode value);

        /**
         * Provide a variable value of any type that would be converted to a json variable and passed to the Flowable application when completing the job.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Success convertAndAddJsonVariable(String name, Object value);
    }
}
