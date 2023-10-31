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
import org.springframework.util.StringUtils;

import org.flowable.external.worker.FlowableWorkerJobListener;
import org.flowable.external.worker.listener.WorkerJobListenerContainer;
import org.flowable.external.worker.worker.FlowableWorkerContainer;

/**
 * Base model for a Flowable worker endpoint.
 *
 * @author Filip Hrisafov
 * @see MethodFlowableWorkerEndpoint
 */
public abstract class AbstractFlowableWorkerEndpoint implements FlowableWorkerEndpoint {

	protected String id = "";

	protected String topic;

	protected Duration lockDuration;

	protected Duration pollingInterval;

	protected Integer numberOfRetries;

	protected Integer numberOfTasks;

	protected Integer concurrency;

	/**
	 * Set a custom id for this endpoint.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the id of this endpoint (possibly generated).
	 */
	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * Set the name of the topic for this endpoint.
	 */
	public void setTopic(String topic) {
		Assert.hasText(topic, "topic must not be empty");
		this.topic = topic;
	}

	/**
	 * Return the name of the topic for this endpoint.
	 */
	public String getTopic() {
		return this.topic;
	}

	/**
	 * Set lock duration when requesting jobs
	 */
	public void setLockDuration(Duration lockDuration) {
		this.lockDuration = lockDuration;
	}

	/**
	 * Return lock duration when requesting jobs, if any.
	 */
	public Duration getLockDuration() {
		return this.lockDuration;
	}

	/**
	 * Set polling interval for requesting jobs
	 */
	public void setPollingInterval(Duration pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Return polling interval for requesting jobs, if any.
	 */
	public Duration getPollingInterval() {
		return this.pollingInterval;
	}

	/**
	 * Set the number of retries that should be sent when requesting jobs.
	 */
	public void setNumberOfRetries(Integer numberOfRetries) {
		this.numberOfRetries = numberOfRetries;
	}

	/**
	 * Return the number of retries that should be sent when requesting jobs, if any.
	 */
	public Integer getNumberOfRetries() {
		return this.numberOfRetries;
	}

	/**
	 * Set the amount of jobs that should be requested, if any.
	 */
	public void setNumberOfTasks(Integer numberOfTasks) {
		this.numberOfTasks = numberOfTasks;
	}

	/**
	 * Return the number of jobs that should be requested, if any.
	 */
	public Integer getNumberOfTasks() {
		return this.numberOfTasks;
	}

	/**
	 * Set the amount of concurrent workers that will be polling for jobs.
	 */
	public void setConcurrency(Integer concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * Return the amount of concurrent workers that will be polling for jobs.
	 */
	public Integer getConcurrency() {
		return concurrency;
	}

	@Override
	public void setupWorkerContainer(FlowableWorkerContainer workerContainer) {
		if (workerContainer instanceof WorkerJobListenerContainer abstractContainer) {
			setupFlowableWorkerContainer(abstractContainer);
		}
		setupWorkerJobListener(workerContainer);
	}

	protected void setupFlowableWorkerContainer(WorkerJobListenerContainer workerContainer) {
		if (StringUtils.hasText(getId())) {
			workerContainer.setBeanName(getId());
		}
		String topic = getTopic();
		Assert.hasText(topic, "topic has to be configured");
		workerContainer.setTopic(topic);

		Integer concurrency = getConcurrency();
		if (concurrency != null) {
			workerContainer.setConcurrency(concurrency);
		}

		Duration pollingInterval = getPollingInterval();
		if (pollingInterval != null) {
			workerContainer.setPollingInterval(pollingInterval);
		}

		Duration lockDuration = getLockDuration();
		if (lockDuration != null) {
			workerContainer.setLockDuration(lockDuration);
		}

		Integer numberOfRetries = getNumberOfRetries();
		if (numberOfRetries != null) {
			workerContainer.setNumberOfRetries(numberOfRetries);
		}

		Integer numberOfTasks = getNumberOfTasks();
		if (numberOfTasks != null) {
			workerContainer.setNumberOfTasks(numberOfTasks);
		}

	}

	/**
	 * Create a {@link FlowableWorkerJobListener} that is able to serve this endpoint for the specified container.
	 */
	protected abstract FlowableWorkerJobListener createWorkerJobListener(FlowableWorkerContainer container);

	protected void setupWorkerJobListener(FlowableWorkerContainer container) {
		container.setupWorkerJobListener(createWorkerJobListener(container));
	}

	/**
	 * Return a description for this endpoint.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected StringBuilder getEndpointDescription() {
		StringBuilder result = new StringBuilder();
		return result.append(getClass().getSimpleName())
				.append('[').append(this.id).append(']')
				.append(" topic='").append(this.topic).append('\'')
				.append(" | lockDuration=").append(this.lockDuration)
				.append(" | numberOfRetries=").append(this.numberOfRetries)
				.append(" | numberOfTasks=").append(this.numberOfRetries);
	}

	@Override
	public String toString() {
		return getEndpointDescription().toString();
	}
}
