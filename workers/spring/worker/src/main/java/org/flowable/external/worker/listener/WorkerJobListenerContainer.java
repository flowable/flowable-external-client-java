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
package org.flowable.external.worker.listener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.ExternalWorkerJobCompletionBuilder;
import org.flowable.external.client.ExternalWorkerJobFailureBuilder;
import org.flowable.external.worker.FlowableWorkerException;
import org.flowable.external.worker.FlowableWorkerJobListener;
import org.flowable.external.worker.WorkerContext;
import org.flowable.external.worker.WorkerContextAwareFlowableWorkerJobListener;
import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.WorkerResultBuilder;
import org.flowable.external.worker.worker.FlowableWorkerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
public class WorkerJobListenerContainer implements FlowableWorkerContainer, BeanNameAware {

    private static final int DEFAULT_SLEEP_INTERVAL = 100;

    private static final int SMALL_SLEEP_INTERVAL = 10;

    private static final long SMALL_INTERVAL_THRESHOLD = 500;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<AsyncTaskExecutor> executors = new ArrayList<>();
    protected final List<CompletableFuture<Void>> runningJobs = new ArrayList<>();

    protected final Object lifecycleMonitor = new Object();
    protected volatile boolean running = false;

    protected String beanName;

    protected int phase = DEFAULT_PHASE;

    protected int concurrency = 1;
    protected int shutdownTimeout = 10_000; // 10 seconds

    protected String topic;
    protected Duration lockDuration = Duration.ofMinutes(5);
    protected int numberOfRetries = 5;
    protected int numberOfTasks = 1;

    protected Duration pollingInterval = Duration.ofSeconds(30);

    protected FlowableWorkerJobListener workerJobListener;

    protected final ExternalWorkerClient externalWorkerClient;

    public WorkerJobListenerContainer(ExternalWorkerClient externalWorkerClient) {
        Assert.notNull(externalWorkerClient, "externalWorkerClient cannot be null");
        this.externalWorkerClient = externalWorkerClient;
    }

    @Override
    public void setupWorkerJobListener(FlowableWorkerJobListener workerJobListener) {
        this.workerJobListener = workerJobListener;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return phase;
    }

    @Override
    public void start() {
        synchronized (this.lifecycleMonitor) {
            if (isRunning()) {
                return;
            }
            Assert.notNull(this.workerJobListener, "A " + FlowableWorkerJobListener.class.getName() + " implementation must be provided");
            Assert.notNull(this.topic, "A topic must be provided");
            running = true;

            String beanName = this.beanName == null ? "worker" : this.beanName;
            for (int i = 0; i < concurrency; i++) {
                AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(beanName + "-" + (i + 1) + "-C-");
                this.executors.add(executor);
                this.runningJobs.add(executor.submitCompletable(new ListenerConsumer(i + 1)));
            }
        }
    }

    @Override
    public void stop() {
        synchronized (this.lifecycleMonitor) {
            if (isRunning()) {
                doStop(() -> {
                });
            }
        }
    }

    @Override
    public void stop(Runnable callback) {
        synchronized (this.lifecycleMonitor) {
            if (isRunning()) {
                doStop(callback);
            } else {
                callback.run();
            }
        }
    }

    protected void doStop(Runnable callback) {
        running = false;
        if (!runningJobs.isEmpty()) {
            try {
                CompletableFuture.allOf(runningJobs.toArray(CompletableFuture[]::new))
                        .get(shutdownTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread has been interrupted", ex);
            } catch (ExecutionException e) {
                logger.error("Exception during shutdown of running workers", e);
            } catch (TimeoutException e) {
                logger.error("Running jobs did not finish within the configured shutdown {}", Duration.ofMillis(shutdownTimeout));
            }
        }

        this.runningJobs.clear();
        this.executors.clear();
        callback.run();
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        Assert.isTrue(concurrency > 0, "concurrency must be greater than 0");
        this.concurrency = concurrency;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        Assert.notNull(topic, "topic must not be null");
        this.topic = topic;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        Assert.notNull(lockDuration, "lockDuration must not be null");
        Assert.state(!lockDuration.isNegative() && !lockDuration.isZero(), "lockDuration must be positive");
        this.lockDuration = lockDuration;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    public void setNumberOfRetries(int numberOfRetries) {
        Assert.state(numberOfRetries > 0, "numberOfRetries must be greater than 0");
        this.numberOfRetries = numberOfRetries;
    }

    public int getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        Assert.state(numberOfTasks > 0, "numberOfTasks must be greater than 0");
        this.numberOfTasks = numberOfTasks;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Duration pollingInterval) {
        Assert.notNull(pollingInterval, "pollingInterval must not be null");
        Assert.state(!pollingInterval.isNegative() && !pollingInterval.isZero(), "pollingInterval must be positive");
        this.pollingInterval = pollingInterval;
    }

    protected class ListenerConsumer implements SchedulingAwareRunnable {

        protected final int index;

        protected ListenerConsumer(int index) {
            this.index = index;
        }

        @Override
        public boolean isLongLived() {
            return true;
        }

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    pollAndInvoke();
                } catch (Error e) {
                    logger.error("Stopping container due to an Error", e);
                    throw e;
                } catch (Exception ex) {
                    logger.error("Consumer exception", ex);
                    doIdle();
                }
            }
        }

        protected void pollAndInvoke() {
            logger.debug("Polling for jobs");
            List<AcquiredExternalWorkerJob> jobs = doPoll();
            logger.debug("Found {} jobs", jobs.size());
            invokeIfHasJobs(jobs);
        }

        protected List<AcquiredExternalWorkerJob> doPoll() {
            return externalWorkerClient.createJobAcquireBuilder()
                    .topic(topic)
                    .lockDuration(lockDuration)
                    .numberOfRetries(numberOfRetries)
                    .numberOfTasks(numberOfTasks)
                    .acquireAndLock();
        }

        protected void invokeIfHasJobs(List<AcquiredExternalWorkerJob> jobs) {
            if (!jobs.isEmpty()) {
                invokeListener(jobs);
            } else {
                doIdle();
            }
        }

        protected void doIdle() {
            try {
                long interval = pollingInterval.toMillis();
                logger.debug("Waiting for {}ms before polling again", interval);
                long timeout = System.currentTimeMillis() + interval;
                long sleepInterval = interval > SMALL_INTERVAL_THRESHOLD ? DEFAULT_SLEEP_INTERVAL : SMALL_SLEEP_INTERVAL;
                do {
                    Thread.sleep(sleepInterval);
                    if (!isRunning()) {
                        break;
                    }
                }
                while (System.currentTimeMillis() < timeout);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Consumer Thread [" + this + "] has been interrupted", ex);
            }

        }

        protected void invokeListener(List<AcquiredExternalWorkerJob> jobs) {
            for (AcquiredExternalWorkerJob job : jobs) {
                if (!isRunning()) {
                    //TODO maybe we need to unacquire the jobs
                    break;
                }

                doInvokeOnListener(job);
            }
        }

        protected void doInvokeOnListener(AcquiredExternalWorkerJob job) {
            logger.debug("Invoking job {} on listener {}", job, workerJobListener);
            try {
                WorkerContextImpl workerContext = new WorkerContextImpl(job);
                if (workerJobListener instanceof WorkerContextAwareFlowableWorkerJobListener contextAwareFlowableWorkerJobListener) {
                    contextAwareFlowableWorkerJobListener.onAcquiredJob(job, workerContext);
                } else {
                    workerJobListener.onAcquiredJob(job);
                }

                WorkerResult result = workerContext.result;
                if (result == null) {
                    externalWorkerClient.createCompletionBuilder(job).complete();
                } else if (result instanceof WorkerSuccessResultImpl success) {
                    success.complete();
                } else if (result instanceof WorkerFailureResultImpl failure) {
                    failure.failureBuilder.fail();
                } else {
                    throw new IllegalStateException("Received an unknown WorkerResult " + result);
                }
            } catch (FlowableWorkerException ex) {
                ExternalWorkerJobFailureBuilder failureBuilder = externalWorkerClient.createFailureBuilder(job)
                        .message(ex.getMessage());

                String errorDetails = ex.getErrorDetails();
                if (StringUtils.hasText(errorDetails)) {
                    failureBuilder.details(errorDetails);
                }

                int retries = ex.getRetries();
                if (retries > 0) {
                    failureBuilder.retries(retries);
                }

                Duration retryTimeout = ex.getRetryTimeout();
                if (retryTimeout != null) {
                    failureBuilder.retryTimeout(retryTimeout);
                }

                failureBuilder.fail();
            } catch (Exception ex) {
                externalWorkerClient.createFailureBuilder(job)
                        .error(ex)
                        .fail();

            }
        }

        @Override
        public String toString() {
            return "FlowableWorkerJobListenerContainer.ListenerConsumer ["
                    + "\ntopic=" + topic
                    + "\nconsumerIndex=" + index
                    + "\n]";
        }
    }

    class WorkerContextImpl implements WorkerContext {

        protected final AcquiredExternalWorkerJob job;
        protected WorkerResult result;

        WorkerContextImpl(AcquiredExternalWorkerJob job) {
            this.job = job;
        }

        @Override
        public WorkerResultBuilder createResultBuilder() {
            return new WorkerResultBuilderImpl(job);
        }

        @Override
        public void markSuccessful(WorkerResult.Success result) {
            if (result == null) {
                throw new IllegalArgumentException("success result is null");
            } else if (result instanceof WorkerSuccessResultImpl) {
                this.result = result;
            } else {
                throw new IllegalArgumentException("Unsupported success result type " + result);
            }
        }

        @Override
        public void markFailed(WorkerResult.Failure result) {
            if (result == null) {
                throw new IllegalArgumentException("failure result is null");
            } else if (result instanceof WorkerFailureResultImpl) {
                this.result = result;
            } else {
                throw new IllegalArgumentException("Unsupported failure result type " + result);
            }
        }
    }

    class WorkerResultBuilderImpl implements WorkerResultBuilder {

        protected final AcquiredExternalWorkerJob job;

        WorkerResultBuilderImpl(AcquiredExternalWorkerJob job) {
            this.job = job;
        }

        @Override
        public WorkerResult.Success success() {
            return new WorkerSuccessResultImpl(externalWorkerClient.createCompletionBuilder(job), ExternalWorkerJobCompletionBuilder::complete);
        }

        @Override
        public WorkerResult.Success bpmnError() {
            return new WorkerSuccessResultImpl(externalWorkerClient.createCompletionBuilder(job), ExternalWorkerJobCompletionBuilder::bpmnError);
        }

        @Override
        public WorkerResult.Success bpmnError(String errorCode) {
            return new WorkerSuccessResultImpl(externalWorkerClient.createCompletionBuilder(job), builder -> builder.bpmnError(errorCode));
        }

        @Override
        public WorkerResult.Success cmmnTerminate() {
            return new WorkerSuccessResultImpl(externalWorkerClient.createCompletionBuilder(job), ExternalWorkerJobCompletionBuilder::cmmnTerminate);
        }

        @Override
        public WorkerResult.Failure failure() {
            return new WorkerFailureResultImpl(externalWorkerClient.createFailureBuilder(job));
        }
    }

    static class WorkerSuccessResultImpl implements WorkerResult.Success {

        protected final ExternalWorkerJobCompletionBuilder completionBuilder;
        protected final Consumer<ExternalWorkerJobCompletionBuilder> completionBuilderFinisher;

        WorkerSuccessResultImpl(ExternalWorkerJobCompletionBuilder completionBuilder, Consumer<ExternalWorkerJobCompletionBuilder> completionBuilderFinisher) {
            this.completionBuilder = completionBuilder;
            this.completionBuilderFinisher = completionBuilderFinisher;
        }

        @Override
        public WorkerResult.Success variable(String name, String value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Short value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Integer value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Long value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Double value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Boolean value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Date value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, Instant value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, LocalDate value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, LocalDateTime value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success variable(String name, JsonNode value) {
            completionBuilder.variable(name, value);
            return this;
        }

        @Override
        public WorkerResult.Success convertAndAddJsonVariable(String name, Object value) {
            completionBuilder.convertAndAddJsonVariable(name, value);
            return this;
        }

        public void complete() {
            completionBuilderFinisher.accept(this.completionBuilder);
        }
    }

    static class WorkerFailureResultImpl implements WorkerResult.Failure {

        protected final ExternalWorkerJobFailureBuilder failureBuilder;

        WorkerFailureResultImpl(ExternalWorkerJobFailureBuilder failureBuilder) {
            this.failureBuilder = failureBuilder;
        }

        @Override
        public WorkerResult.Failure error(Exception exception) {
            failureBuilder.error(exception);
            return this;
        }

        @Override
        public WorkerResult.Failure message(String message) {
            failureBuilder.message(message);
            return this;
        }

        @Override
        public WorkerResult.Failure details(String details) {
            failureBuilder.details(details);
            return this;
        }

        @Override
        public WorkerResult.Failure retries(int retries) {
            failureBuilder.retries(retries);
            return this;
        }

        @Override
        public WorkerResult.Failure retryTimeout(Duration retryTimeout) {
            failureBuilder.retryTimeout(retryTimeout);
            return this;
        }
    }

}
