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
package org.flowable.external.worker.listener.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.FlowableWorkerException;
import org.flowable.external.worker.WorkerContext;
import org.flowable.external.worker.WorkerContextAwareFlowableWorkerJobListener;
import org.flowable.external.worker.WorkerResult;

/**
 * @author Filip Hrisafov
 */
public class MessagingWorkerJobListenerAdapter implements WorkerContextAwareFlowableWorkerJobListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final InvocableHandlerMethod handlerMethod;

    public MessagingWorkerJobListenerAdapter(InvocableHandlerMethod handlerMethod) {
        Assert.notNull(handlerMethod, "handlerMethod is null");
        this.handlerMethod = handlerMethod;
    }

    @Override
    public void onAcquiredJob(AcquiredExternalWorkerJob job, WorkerContext workerContext) {
        Message<?> message = toMessagingMessage(job);
        logger.debug("Processing [{}]", message);

        Object result = invokeHandler(job, message, workerContext);
        if (result != null) {
            handleResult(result, job, workerContext);
        } else {
            logger.trace("No result object given - no result to handle");
        }
    }

    protected Message<?> toMessagingMessage(AcquiredExternalWorkerJob job) {
        return MessageBuilder.withPayload(job).build();
    }

    protected Object invokeHandler(AcquiredExternalWorkerJob job, Message<?> message, WorkerContext workerContext) {
        try {
            return handlerMethod.invoke(message, job, workerContext.createResultBuilder());
        } catch (MessagingException ex) {
            throw new ListenerExceptionFailedException(createMessagingErrorMessage("Listener method could not be invoked with incoming message"), ex);
        } catch (FlowableWorkerException ex) {
            // This is a business exception that gets rethrown
            throw ex;
        } catch (Exception ex) {
            throw new ListenerExceptionFailedException(
                    createMessagingErrorMessage("Listener method '" + handlerMethod.getMethod().toGenericString() + "' threw exception"), ex);
        }
    }

    protected void handleResult(Object result, AcquiredExternalWorkerJob job, WorkerContext workerContext) {
        if (result instanceof WorkerResult.Success success) {
            workerContext.markSuccessful(success);
        } else if (result instanceof WorkerResult.Failure failure) {
            workerContext.markFailed(failure);
        }
    }

    protected String createMessagingErrorMessage(String description) {
        return description + '\n'
                + "Endpoint handler details:\n"
                + "Method [" + handlerMethod.getMethod() + "]\n"
                + "Bean [" + handlerMethod.getBean() + "]\n";
    }
}
