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

import java.lang.reflect.Method;

import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;

import org.flowable.external.worker.FlowableWorkerJobListener;
import org.flowable.external.worker.listener.adapter.MessagingWorkerJobListenerAdapter;
import org.flowable.external.worker.worker.FlowableWorkerContainer;

/**
 * A {@link FlowableWorkerEndpoint} providing the method to invoke to process an incoming message for this endpoint.
 *
 * @author Filip Hrisafov
 */
public class MethodFlowableWorkerEndpoint extends AbstractFlowableWorkerEndpoint {

	private Object bean;

	private Method method;

	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	/**
	 * Set the actual bean instance to invoke this endpoint method on.
	 */
	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Object getBean() {
		return this.bean;
	}

	/**
	 * Set the method to invoke for processing a job managed by this endpoint.
	 */
	public void setMethod(Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return this.method;
	}

	/**
	 * Set the {@link MessageHandlerMethodFactory} to use to build the
	 * {@link InvocableHandlerMethod} responsible to manage the invocation
	 * of this endpoint.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	@Override
	protected FlowableWorkerJobListener createWorkerJobListener(FlowableWorkerContainer container) {
		Assert.state(this.messageHandlerMethodFactory != null,
				"Could not create worker job listener - MessageHandlerMethodFactory not set");
		Object bean = getBean();
		Method method = getMethod();
		Assert.state(bean != null && method != null, "No bean+method set on endpoint");
		InvocableHandlerMethod invocableHandlerMethod =
				this.messageHandlerMethodFactory.createInvocableHandlerMethod(bean, method);
		return new MessagingWorkerJobListenerAdapter(invocableHandlerMethod);
	}

	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | bean='").append(this.bean).append('\'')
				.append(" | method='").append(this.method).append('\'');
	}

}
