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
package org.flowable.external.worker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;

/**
 * @author Filip Hrisafov
 */
@Component
public class SimpleWorkerWithAnnotationFixValues {

    protected final List<AcquiredExternalWorkerJob> receivedJobs = new CopyOnWriteArrayList<>();

    @FlowableWorker(topic = "simpleWithProperties", lockDuration = "PT10M", numberOfTasks = "3", numberOfRetries = "1", pollingInterval = "PT0.5S")
    public void processJob(AcquiredExternalWorkerJob job) {
        receivedJobs.add(job);
    }

    public List<AcquiredExternalWorkerJob> getReceivedJobs() {
        return receivedJobs;
    }
}
