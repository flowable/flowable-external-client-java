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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import tools.jackson.databind.JsonNode;

/**
 * A builder that can be used to complete a specific job.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobCompletionBuilder {

    /**
     * Provide a {@link String} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, String value);

    /**
     * Provide a {@link Short} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Short value);

    /**
     * Provide an {@link Integer} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Integer value);

    /**
     * Provide a {@link Long} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Long value);

    /**
     * Provide a {@link Double} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Double value);

    /**
     * Provide a {@link Boolean} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Boolean value);

    /**
     * Provide a {@link Date} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Date value);

    /**
     * Provide an {@link Instant} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, Instant value);

    /**
     * Provide a {@link LocalDate} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, LocalDate value);

    /**
     * Provide a {@link LocalDateTime} variable that would be passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, LocalDateTime value);

    /**
     * Provide a {@link JsonNode} that would be passed as a json variable to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder variable(String name, JsonNode value);

    /**
     * Provide a variable value of any type that would be converted to a json variable and passed to the Flowable application when completing the job.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    ExternalWorkerJobCompletionBuilder convertAndAddJsonVariable(String name, Object value);

    /**
     * Execute the completion of the job.
     */
    void complete();

    /**
     * Execute the completion of the job using a BPMN error without an error code.
     *
     * @see #bpmnError(String)
     */
    void bpmnError();

    /**
     * Execute the completion of the job using a BPMN error using the given error code.
     *
     * @see #bpmnError()
     */
    void bpmnError(String errorCode);

    /**
     * Execute the completion of the job using a CMMN termination.
     */
    void cmmnTerminate();
}
