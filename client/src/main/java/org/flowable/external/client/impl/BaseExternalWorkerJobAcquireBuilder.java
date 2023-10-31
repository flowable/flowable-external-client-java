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
package org.flowable.external.client.impl;

import java.time.Duration;
import java.util.List;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.client.ExternalWorkerJobAcquireBuilder;

/**
 * @author Filip Hrisafov
 */
public abstract class BaseExternalWorkerJobAcquireBuilder implements ExternalWorkerJobAcquireBuilder {

    protected String topic;
    protected Duration lockDuration;
    protected String scopeType;
    protected int numberOfTasks;
    protected int numberOfRetries;

    @Override
    public ExternalWorkerJobAcquireBuilder topic(String topic) {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("topic is empty");
        }
        this.topic = topic;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder lockDuration(Duration lockDuration) {
        if (lockDuration == null) {
            throw new IllegalArgumentException("lockDuration is null");
        }

        if (lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("lockDuration must be positive");
        }
        this.lockDuration = lockDuration;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder onlyBpmn() {
        if ("cmmn".equals(scopeType)) {
            throw new IllegalArgumentException("Cannot combine onlyCmmn() with onlyBpmn() in the same query");
        }

        if (scopeType != null) {
            throw new IllegalArgumentException("Cannot combine scopeType(String) with onlyBpmn() in the same query");
        }
        return scopeType("bpmn");
    }

    @Override
    public ExternalWorkerJobAcquireBuilder onlyCmmn() {
        if ("bpmn".equals(scopeType)) {
            throw new IllegalArgumentException("Cannot combine onlyBpmn() with onlyCmmn() in the same query");
        }

        if (scopeType != null) {
            throw new IllegalArgumentException("Cannot combine scopeType(String) with onlyCmmn() in the same query");
        }
        return scopeType("cmmn");
    }

    @Override
    public ExternalWorkerJobAcquireBuilder scopeType(String scopeType) {
        if (scopeType == null || scopeType.isEmpty()) {
            throw new IllegalArgumentException("scopeType is empty");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder numberOfTasks(int numberOfTasks) {
        if (numberOfTasks <= 0) {
            throw new IllegalArgumentException("numberOfTasks must be positive");
        }
        this.numberOfTasks = numberOfTasks;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder numberOfRetries(int numberOfRetries) {
        if (numberOfRetries <= 0) {
            throw new IllegalArgumentException("numberOfRetries must be positive");
        }
        this.numberOfRetries = numberOfRetries;
        return this;
    }

    @Override
    public final List<AcquiredExternalWorkerJob> acquireAndLock() {
        if (topic == null) {
            throw new IllegalArgumentException("topic has to be provided");
        }
        return acquireAndLockInternal();
    }

    protected abstract List<AcquiredExternalWorkerJob> acquireAndLockInternal();
}
