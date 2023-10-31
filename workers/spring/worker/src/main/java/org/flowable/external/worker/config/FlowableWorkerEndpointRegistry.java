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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import org.flowable.external.worker.worker.FlowableWorkerContainer;

/**
 * @author Filip Hrisafov
 */
public class FlowableWorkerEndpointRegistry implements DisposableBean, SmartLifecycle, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, FlowableWorkerContainer> workerContainers = new ConcurrentHashMap<>();
    protected int phase = DEFAULT_PHASE;

    protected ApplicationContext applicationContext;
    protected boolean contextRefreshed;
    private volatile boolean running;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.applicationContext) {
            this.contextRefreshed = true;
        }
    }

    /**
     * Return the {@link FlowableWorkerContainer} with the specified id or
     * {@code null} if no such container exists.
     *
     * @param id the id of the container
     * @return the container or {@code null} if no container with that id exists
     * @see FlowableWorkerEndpoint#getId()
     * @see #getWorkerContainerIds()
     */
    @Nullable
    public FlowableWorkerContainer getWorkerContainer(String id) {
        Assert.notNull(id, "Container identifier must not be null");
        return this.workerContainers.get(id);
    }

    /**
     * Return the ids of the managed {@link FlowableWorkerContainer} instance(s).
     *
     * @see #getWorkerContainer(String)
     */
    public Set<String> getWorkerContainerIds() {
        return Collections.unmodifiableSet(this.workerContainers.keySet());
    }

    /**
     * Return the managed {@link FlowableWorkerContainer} instance(s).
     */
    public Collection<FlowableWorkerContainer> getWorkerContainers() {
        return Collections.unmodifiableCollection(this.workerContainers.values());
    }

    /**
     * Create a worker container for the given {@link FlowableWorkerEndpoint}.
     * <p>This create the necessary infrastructure to honor that endpoint with regard to its configuration.
     * <p>The {@code startImmediately} flag determines if the container should be started immediately.
     *
     * @param endpoint the endpoint to add
     * @param factory the worker factory to use
     * @param startImmediately start the container immediately if necessary
     * @see #getWorkerContainers()
     * @see #getWorkerContainer(String)
     */
    public void registerWorkerContainer(FlowableWorkerEndpoint endpoint, FlowableWorkerContainerFactory<?> factory, boolean startImmediately) {

        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.notNull(factory, "Factory must not be null");
        String id = endpoint.getId();
        Assert.hasText(id, "Endpoint id must be set");

        synchronized (this.workerContainers) {
            if (this.workerContainers.containsKey(id)) {
                throw new IllegalStateException("Another endpoint is already registered with id '" + id + "'");
            }
            FlowableWorkerContainer container = createWorkerContainer(endpoint, factory);
            this.workerContainers.put(id, container);
            if (startImmediately) {
                startIfNecessary(container);
            }
        }
    }

    /**
     * Create a worker container for the given {@link FlowableWorkerEndpoint}.
     * <p>This create the necessary infrastructure to honor that endpoint with regard to its configuration.
     *
     * @param endpoint the endpoint to add
     * @param factory the worker factory to use
     * @see #registerWorkerContainer(FlowableWorkerEndpoint, FlowableWorkerContainerFactory, boolean)
     */
    public void registerWorkerContainer(FlowableWorkerEndpoint endpoint, FlowableWorkerContainerFactory<?> factory) {
        registerWorkerContainer(endpoint, factory, false);
    }

    /**
     * Create and start a new container using the specified factory.
     */
    protected FlowableWorkerContainer createWorkerContainer(FlowableWorkerEndpoint endpoint,
            FlowableWorkerContainerFactory<?> factory) {

        FlowableWorkerContainer workerContainer = factory.createWorkerContainer(endpoint);

        if (workerContainer instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
            } catch (Exception ex) {
                throw new BeanInitializationException("Failed to initialize worker container", ex);
            }
        }

        int containerPhase = workerContainer.getPhase();
        if (containerPhase < Integer.MAX_VALUE) {  // a custom phase value
            if (this.phase < Integer.MAX_VALUE && this.phase != containerPhase) {
                throw new IllegalStateException("Encountered phase mismatch between container factory definitions: " +
                        this.phase + " vs " + containerPhase);
            }
            this.phase = workerContainer.getPhase();
        }

        return workerContainer;
    }

    // Delegating implementation of SmartLifecycle

    @Override
    public int getPhase() {
        return this.phase;
    }

    @Override
    public void start() {
        for (FlowableWorkerContainer workerContainer : getWorkerContainers()) {
            startIfNecessary(workerContainer);
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        for (FlowableWorkerContainer workerContainer : getWorkerContainers()) {
            workerContainer.stop();
        }
    }

    @Override
    public void stop(Runnable callback) {
        this.running = false;
        Collection<FlowableWorkerContainer> workerContainers = getWorkerContainers();
        if (!workerContainers.isEmpty()) {
            AggregatingCallback aggregatingCallback = new AggregatingCallback(workerContainers.size(), callback);
            for (FlowableWorkerContainer workerContainer : workerContainers) {
                if (workerContainer.isRunning()) {
                    workerContainer.stop(aggregatingCallback);
                } else {
                    aggregatingCallback.run();
                }
            }
        } else {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Start the specified {@link FlowableWorkerContainer} if it should be started
     * on startup or when start is called explicitly after startup.
     *
     * @see FlowableWorkerContainer#isAutoStartup()
     */
    protected void startIfNecessary(FlowableWorkerContainer workerContainer) {
        if (this.contextRefreshed || workerContainer.isAutoStartup()) {
            workerContainer.start();
        }
    }

    @Override
    public void destroy() {
        for (FlowableWorkerContainer workerContainer : getWorkerContainers()) {
            if (workerContainer instanceof DisposableBean disposableBean) {
                try {
                    disposableBean.destroy();
                } catch (Throwable ex) {
                    logger.warn("Failed to destroy worker container", ex);
                }
            }
        }
    }

    private static class AggregatingCallback implements Runnable {

        private final AtomicInteger count;

        private final Runnable finishCallback;

        public AggregatingCallback(int count, Runnable finishCallback) {
            this.count = new AtomicInteger(count);
            this.finishCallback = finishCallback;
        }

        @Override
        public void run() {
            if (this.count.decrementAndGet() == 0) {
                this.finishCallback.run();
            }
        }
    }
}
