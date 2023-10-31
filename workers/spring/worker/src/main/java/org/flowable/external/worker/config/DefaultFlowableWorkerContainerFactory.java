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
package org.flowable.external.worker.config;

import java.time.Duration;

import org.springframework.util.Assert;

import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.worker.listener.WorkerJobListenerContainer;

/**
 * @author Filip Hrisafov
 */
public class DefaultFlowableWorkerContainerFactory implements FlowableWorkerContainerFactory<WorkerJobListenerContainer> {

    protected ExternalWorkerClient externalWorkerClient;

    protected Duration pollingInterval;
    protected Integer concurrency;

    protected Duration lockDuration;
    protected Integer numberOfTasks;
    protected Integer numberOfRetries;

    public void setExternalWorkerClient(ExternalWorkerClient externalWorkerClient) {
        this.externalWorkerClient = externalWorkerClient;
    }

    /**
     * Specify the container polling interval
     *
     * @param pollingInterval the polling interval for the container
     */
    public void setPollingInterval(Duration pollingInterval) {
        Assert.notNull(pollingInterval, "pollingInterval must not be null");
        Assert.state(!pollingInterval.isNegative() && !pollingInterval.isZero(), "pollingInterval must be positive");
        this.pollingInterval = pollingInterval;
    }

    /**
     * Specify the container concurrency
     *
     * @param concurrency the number of consumers to create
     */
    public void setConcurrency(Integer concurrency) {
        Assert.isTrue(concurrency == null || concurrency > 0, "concurrency must be greater than 0");
        this.concurrency = concurrency;
    }

    /**
     * Specify the lock duration for the acquired jobs
     *
     * @param lockDuration the lock duration for the acquired jobs
     */
    public void setLockDuration(Duration lockDuration) {
        Assert.state(lockDuration == null || !lockDuration.isNegative() && !lockDuration.isZero(), "lockDuration must be positive");
        this.lockDuration = lockDuration;
    }

    /**
     * Specify the amount of jobs that should be acquired
     *
     * @param numberOfTasks the amount of jobs that should be acquired
     */
    public void setNumberOfTasks(Integer numberOfTasks) {
        Assert.state(numberOfTasks == null || numberOfTasks > 0, "numberOfTasks should be greater than 0");
        this.numberOfTasks = numberOfTasks;
    }

    /**
     * Specify the amount of retries when acquiring jobs
     *
     * @param numberOfRetries the amount of retries when acquiring jobs
     */
    public void setNumberOfRetries(Integer numberOfRetries) {
        Assert.state(numberOfRetries == null || numberOfRetries > 0, "numberOfTasks should be greater than 0");
        this.numberOfRetries = numberOfRetries;
    }

    @Override
    public WorkerJobListenerContainer createWorkerContainer(FlowableWorkerEndpoint endpoint) {
        WorkerJobListenerContainer container = new WorkerJobListenerContainer(externalWorkerClient);

        if (concurrency != null) {
            container.setConcurrency(concurrency);
        }

        if (pollingInterval != null) {
            container.setPollingInterval(pollingInterval);
        }

        if (lockDuration != null) {
            container.setLockDuration(lockDuration);
        }

        if (numberOfTasks != null) {
            container.setNumberOfTasks(numberOfTasks);
        }

        if (numberOfRetries != null) {
            container.setNumberOfRetries(numberOfRetries);
        }

        endpoint.setupWorkerContainer(container);
        return container;
    }
}
