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

/**
 * Defines a Java API that can be used to write clients for the external worker jobs in Flowable.
 * It provides a basic API for acquiring, completing and failing jobs.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerClient {

    /**
     * Create a builder that can be used to acquire jobs from a Flowable application.
     */
    ExternalWorkerJobAcquireBuilder createJobAcquireBuilder();

    /**
     * Create a builder that can be used to complete the given job
     *
     * @param job the job that should be completed
     */
    ExternalWorkerJobCompletionBuilder createCompletionBuilder(AcquiredExternalWorkerJob job);

    /**
     * Create a builder that can be used to fail the given job
     *
     * @param job the job that should be failed
     */
    ExternalWorkerJobFailureBuilder createFailureBuilder(AcquiredExternalWorkerJob job);

}
