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
import java.util.List;

/**
 * A builder providing configuration options for acquiring external worker jobs.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobAcquireBuilder {

    /**
     * The topic for the requested jobs
     *
     * @param topic the topic of the jobs
     */
    ExternalWorkerJobAcquireBuilder topic(String topic);

    /**
     * How long should the acquired jobs be locked for.
     *
     * @param lockDuration the duration for locking the jobs
     */
    ExternalWorkerJobAcquireBuilder lockDuration(Duration lockDuration);

    /**
     * Acquire only jobs which are linked to a process instance.
     * Cannot be combined with {@link #onlyCmmn()} and {@link #scopeType(String)}
     */
    ExternalWorkerJobAcquireBuilder onlyBpmn();

    /**
     * Acquire only jobs which are linked to a case instance.
     * Cannot be combined with {@link #onlyBpmn()} and {@link #scopeType(String)}
     */
    ExternalWorkerJobAcquireBuilder onlyCmmn();

    /**
     * Acquire only jobs which are linked to the given scope type.
     * Cannot be combined with {@link #onlyBpmn()} or {@link #onlyCmmn()}
     */
    ExternalWorkerJobAcquireBuilder scopeType(String scopeType);

    /**
     * The number of tasks that should be acquired.
     */
    ExternalWorkerJobAcquireBuilder numberOfTasks(int numberOfTasks);

    /**
     * The number of retries in case an optimistic lock exception occurs during acquiring.
     */
    ExternalWorkerJobAcquireBuilder numberOfRetries(int numberOfRetries);

    /**
     * Acquire and lock the configured amount of jobs.
     */
    List<AcquiredExternalWorkerJob> acquireAndLock();

}
