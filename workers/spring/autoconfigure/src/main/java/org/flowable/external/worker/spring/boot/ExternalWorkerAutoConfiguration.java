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
package org.flowable.external.worker.spring.boot;

import org.flowable.external.client.ExternalWorkerClient;
import org.flowable.external.client.impl.RestExternalWorkerClient;
import org.flowable.external.client.impl.RestInvoker;
import org.flowable.external.worker.annotation.EnableFlowableWorker;
import org.flowable.external.worker.config.DefaultFlowableWorkerContainerFactory;
import org.flowable.external.worker.config.FlowableWorkerConfigUtils;
import org.flowable.external.worker.config.FlowableWorkerContainerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import tools.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@AutoConfiguration
@ConditionalOnClass(EnableFlowableWorker.class)
@EnableConfigurationProperties({
        ExternalWorkerProperties.class
})
@Import({
        RestInvokerConfigurations.JavaHttpClientConfiguration.class
})
public class ExternalWorkerAutoConfiguration {

    protected final ExternalWorkerProperties properties;

    public ExternalWorkerAutoConfiguration(ExternalWorkerProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "flowableWorkerContainerFactory")
    public FlowableWorkerContainerFactory<?> flowableWorkerContainerFactory(ExternalWorkerClient externalWorkerClient) {
        DefaultFlowableWorkerContainerFactory workContainerFactory = new DefaultFlowableWorkerContainerFactory();
        workContainerFactory.setExternalWorkerClient(externalWorkerClient);

        PropertyMapper propertyMapper = PropertyMapper.get();
        propertyMapper.from(properties.getConcurrency()).to(workContainerFactory::setConcurrency);
        propertyMapper.from(properties.getLockDuration()).to(workContainerFactory::setLockDuration);
        propertyMapper.from(properties.getNumberOfRetries()).to(workContainerFactory::setNumberOfRetries);
        propertyMapper.from(properties.getNumberOfTasks()).to(workContainerFactory::setNumberOfTasks);
        propertyMapper.from(properties.getPollingInterval()).to(workContainerFactory::setPollingInterval);
        return workContainerFactory;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalWorkerClient externalWorkerClient(RestInvoker restInvoker, ObjectProvider<ObjectMapper> objectMapper) {
        return RestExternalWorkerClient.create(properties.getWorkerId(), restInvoker, objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableFlowableWorker
    @ConditionalOnMissingBean(name = FlowableWorkerConfigUtils.FLOWABLE_WORKER_ANNOTATION_PROCESSOR_BEAN_NAME)
    static class EnableFlowableWorkerConfiguration {

    }

}
