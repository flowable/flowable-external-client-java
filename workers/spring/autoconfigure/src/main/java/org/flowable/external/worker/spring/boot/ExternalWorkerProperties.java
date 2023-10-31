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

import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.external.worker")
public class ExternalWorkerProperties {

    /**
     * The id of the worker that would be used for polling jobs.
     */
    protected String workerId = UUID.randomUUID().toString();

    /**
     * The default amount of concurrent workers.
     */
    protected Integer concurrency;

    /**
     * The default amount for the lock duration of the acquired jobs.
     */
    protected Duration lockDuration;

    /**
     * The default amount of times the Flowable application should retry to lock the acquired jobs.
     */
    protected Integer numberOfRetries;

    /**
     * The default amount of jobs that should be acquired.
     */
    protected Integer numberOfTasks;

    /**
     * The default polling interval for the workers.
     */
    protected Duration pollingInterval;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public Integer getNumberOfRetries() {
        return numberOfRetries;
    }

    public void setNumberOfRetries(Integer numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    public Integer getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(Integer numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
}
