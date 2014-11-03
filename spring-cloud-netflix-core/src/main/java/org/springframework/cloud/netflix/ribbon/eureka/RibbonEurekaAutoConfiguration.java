/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.cloud.netflix.ribbon.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.discovery.EurekaClientConfig;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(DiscoveryEnabledNIWSServerList.class)
@ConditionalOnBean(SpringClientFactory.class)
@ConditionalOnExpression("${ribbon.eureka.enabled:true}")
@AutoConfigureAfter(RibbonAutoConfiguration.class)
public class RibbonEurekaAutoConfiguration {
	
	@Autowired(required=false)
	private EurekaClientConfig clientConfig;

    @Bean
    public RibbonClientPreprocessor ribbonClientPreprocessor(SpringClientFactory clientFactory) {
        return new EurekaRibbonClientPreprocessor(clientConfig, clientFactory);
    }
}
