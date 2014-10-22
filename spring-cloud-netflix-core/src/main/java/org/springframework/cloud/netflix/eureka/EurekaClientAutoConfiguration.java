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
package org.springframework.cloud.netflix.eureka;

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.JsonXStream;
import com.netflix.discovery.converters.XmlXStream;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnExpression("${eureka.client.enabled:true}")
public class EurekaClientAutoConfiguration {
	
	@PostConstruct
	public void init() {
		XmlXStream.getInstance().setMarshallingStrategy(new DataCenterAwareMarshallingStrategy());
		JsonXStream.getInstance().setMarshallingStrategy(new DataCenterAwareMarshallingStrategy());		
	}

	@Bean
	@ConditionalOnMissingBean(EurekaClientConfig.class)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaInstanceConfig.class)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		return new EurekaInstanceConfigBean();
	}
}
