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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import tools.jackson.databind.node.ObjectNode;
import org.flowable.external.client.impl.RestInvoker;
import org.flowable.external.client.impl.RestResponse;

/**
 * @author Filip Hrisafov
 */
public class StubRestInvoker implements RestInvoker {

    protected static final Map<RequestType, RestResponse<String>> DEFAULT_RESPONSES = Map.of(
            RequestType.ACQUIRE_JOBS, new StubRestResponse(200, "[]"),
            RequestType.COMPLETE_JOB, new StubRestResponse(204, ""),
            RequestType.FAIL_JOB, new StubRestResponse(204, "")
    );

    protected final Map<RequestType, Queue<RestResponse<String>>> responses = new ConcurrentHashMap<>();
    protected final Map<RequestType, List<StubRequest>> requests = new ConcurrentHashMap<>();

    @Override
    public RestResponse<String> post(String path, ObjectNode body) {
        if ("/acquire/jobs".equals(path)) {
            requests.computeIfAbsent(RequestType.ACQUIRE_JOBS, k -> new CopyOnWriteArrayList<>())
                    .add(new StubRequest(path, body));
            Queue<RestResponse<String>> acquireJobsResponses = responses.get(RequestType.ACQUIRE_JOBS);
            if (acquireJobsResponses == null || acquireJobsResponses.isEmpty()) {
                return new StubRestResponse(200, "[]");
            }
            return acquireJobsResponses.poll();

        } else if (path.endsWith("/fail")) {
            requests.computeIfAbsent(RequestType.FAIL_JOB, k -> new CopyOnWriteArrayList<>())
                    .add(new StubRequest(path, body));
            Queue<RestResponse<String>> failJobResponses = responses.get(RequestType.FAIL_JOB);
            if (failJobResponses == null || failJobResponses.isEmpty()) {
                return new StubRestResponse(204, "");
            }

            return failJobResponses.poll();

        } else {
            RequestType requestType;
            if (path.endsWith("/complete")) {
                requestType = RequestType.COMPLETE_JOB;
            } else if (path.endsWith("/bpmnError")) {
                requestType = RequestType.BPMN_ERROR;
            } else if (path.endsWith("/cmmnTerminate")) {
                requestType = RequestType.CMMN_TERMINATE;
            } else {
                throw new UnsupportedOperationException("Path " + path + " is not supported");
            }

            requests.computeIfAbsent(requestType, k -> new CopyOnWriteArrayList<>())
                    .add(new StubRequest(path, body));
            Queue<RestResponse<String>> completeJobResponses = responses.get(requestType);
            if (completeJobResponses == null || completeJobResponses.isEmpty()) {
                return new StubRestResponse(204, "");
            }

            return completeJobResponses.poll();
        }
    }

    protected RestResponse<String> handRequest(RequestType type, String path, ObjectNode body) {
        requests.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(new StubRequest(path, body));
        Queue<RestResponse<String>> requestResponses = responses.get(type);
        if (requestResponses == null || requestResponses.isEmpty()) {
            return DEFAULT_RESPONSES.get(type);
        }

        return requestResponses.poll();
    }

    public void addAcquireJob(String jobId, String workerId) {
        addAcquireResponse(200, """
                [
                  {
                    "id": "%s",
                    "lockOwner": "%s"
                  }
                ]
                """.formatted(jobId, workerId));
    }

    public void addAcquireResponse(int status, String body) {
        addResponse(RequestType.ACQUIRE_JOBS, status, body);
    }

    public void addCompleteResponse(int status, String body) {
        addResponse(RequestType.COMPLETE_JOB, status, body);
    }

    public void addFailResponse(int status, String body) {
        addResponse(RequestType.FAIL_JOB, status, body);
    }

    protected void addResponse(RequestType requestType, int status, String body) {
        responses.computeIfAbsent(requestType, k -> new ConcurrentLinkedQueue<>()).add(new StubRestResponse(status, body));
    }

    public List<StubRequest> getAcquireJobsRequests() {
        return requests.getOrDefault(RequestType.ACQUIRE_JOBS, Collections.emptyList());
    }

    public List<StubRequest> getCompleteJobRequests() {
        return requests.getOrDefault(RequestType.COMPLETE_JOB, Collections.emptyList());
    }

    public List<StubRequest> getBpmnErrorRequests() {
        return requests.getOrDefault(RequestType.BPMN_ERROR, Collections.emptyList());
    }

    public List<StubRequest> getCmmnTerminateRequests() {
        return requests.getOrDefault(RequestType.CMMN_TERMINATE, Collections.emptyList());
    }

    public List<StubRequest> getFailJobRequests() {
        return requests.getOrDefault(RequestType.FAIL_JOB, Collections.emptyList());
    }

    record StubRestResponse(int statusCode, String body) implements RestResponse<String> {

    }

    protected enum RequestType {
        ACQUIRE_JOBS,
        COMPLETE_JOB,
        BPMN_ERROR,
        CMMN_TERMINATE,
        FAIL_JOB,
    }

    public record StubRequest(String path, ObjectNode body) {

    }

}
