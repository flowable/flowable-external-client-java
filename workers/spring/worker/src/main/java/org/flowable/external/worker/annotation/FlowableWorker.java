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
package org.flowable.external.worker.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.WorkerResultBuilder;
import org.springframework.messaging.handler.annotation.MessageMapping;

import org.flowable.external.worker.config.FlowableWorkerEndpointRegistry;

/**
 * Annotation that marks a method to be the target of an External Worker Job
 * for the specific {@link #topic()}.
 *
 * <p> Processing of {@code FlowableWorker} annotations is performed by registering a {@link FlowableWorkerAnnotationBeanPostProcessor}.
 * This can be done manually or through the {@link EnableFlowableWorker @EnableFlowableWorker} annotation.
 *
 * <p>Annotation Flowable Worker methods are allowed to have flexible signatures.
 * Similar to what {@link MessageMapping @MessageMapping} provides.
 *
 * <p>Annotated methods may have a non-{@code void} return type.
 * More specifically they may return {@link WorkerResult WorkerResult}.
 * This allows to pass additional variables when completing the job by using {@link WorkerResult.Success WorkerResult.Success}
 * or additional information when failing the job using {@link WorkerResult.Failure WorkerResult.Failure}.
 * An instance of the specific result can be created by using {@link WorkerResultBuilder WorkerResultBuilder} as a parameter of the method.
 * e.g.
 *
 * <pre><code>
 * package com.example.demo
 *
 * &#064;Component
 * public class ExampleWorker {
 *
 *     &#064;FlowableWorker(topic = "myTopic")
 *     public WorkerResult processJob(AcquiredExternalWorkerJob job, WorkerResultBuilder resultBuilder) {
 *         System.out.println("Executed job: " + job.getId());
 *         Object errorMessage = job.getVariables().get("errorMessage");
 *         if (errorMessage != null) {
 *             return resultBuilder.failure()
 *                     .message(errorMessage.toString())
 *                     .details("Error message details");
 *         }
 *
 *         return resultBuilder.success()
 *                 .variable("message", "Job has been executed");
 *     }
 * }</code></pre>
 *
 * @author Filip Hrisafov
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(FlowableWorkers.class)
@MessageMapping
public @interface FlowableWorker {

    /**
     * The unique identifier of the container managing this endpoint.
     * <p>If none is specified, an auto-generated one is provided.
     *
     * @see FlowableWorkerEndpointRegistry#getWorkerContainer(String)
     */
    String id() default "";

    /**
     * The topic that should be handled by the worker
     */
    String topic();

    /**
     * The duration for the lock when acquiring jobs.
     * If not specified then the global configured default will be used.
     */
    String lockDuration() default "";

    /**
     * The number of retries when acquiring jobs.
     * If not specified then the global configured default will be used.
     */
    String numberOfRetries() default "";

    /**
     * The number of tasks that should be acquired .
     * If not specified then the global configured default will be used.
     */
    String numberOfTasks() default "";

    /**
     * Override the container factory's {@code pollingInterval} setting for this worker.
     */
    String pollingInterval() default "";

    /**
     * Override the container factory's {@code concurrency} setting for this worker.
     */
    String concurrency() default "";

}
