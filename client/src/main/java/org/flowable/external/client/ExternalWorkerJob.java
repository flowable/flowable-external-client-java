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

/**
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJob {

    /**
     * Returns the unique identifier for this job.
     */
    String getId();

    /**
     * Returns the correlation id of a job.
     * The same job can be moved around and have its technical id changed.
     * This id allows tracking that job.
     */
    String getCorrelationId();

    /**
     * Returns the number of retries this job has left. Whenever the jobexecutor fails to execute the job, this value is decremented.
     * When it hits zero, the job is supposed to be dead and not retried again (ie a manual retry is required then).
     */
    int getRetries();

    /**
     * Returns the date on which this job is supposed to be processed.
     */
    Instant getDueDate();

    /**
     * Returns the message of the exception that occurred, the last time the job was executed. Returns null when no exception occurred.
     * <p>
     * To get the full exception stacktrace, use ManagementService#getJobExceptionStacktrace(String)
     */
    String getExceptionMessage();

    /**
     * Get the tenant identifier for this job.
     */
    String getTenantId();

    /**
     * Reference to an element identifier or null if none is set.
     */
    String getElementId();

    /**
     * Reference to an element name or null if none is set.
     */
    String getElementName();

    /**
     * Reference to a scope identifier or null if none is set.
     */
    String getScopeId();

    /**
     * Reference to a sub scope identifier or null if none is set.
     */
    String getSubScopeId();

    /**
     * Reference to a scope type or null if none is set.
     */
    String getScopeType();

    /**
     * Reference to a scope definition identifier or null if none is set.
     */
    String getScopeDefinitionId();

    /**
     * Returns the creation datetime of the job.
     */
    Instant getCreateTime();

    /**
     * The id of the worker that has the lock on the job
     */
    String getWorkerId();

    /**
     * The time when the lock expires
     */
    Instant getLockExpirationTime();

}
