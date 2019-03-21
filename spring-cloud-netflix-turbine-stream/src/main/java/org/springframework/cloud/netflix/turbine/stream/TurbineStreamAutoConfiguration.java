/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.turbine.stream;

import java.util.Map;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import rx.subjects.PublishSubject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for a Spring Cloud Turbine using Spring Cloud Stream. Enabled by
 * default if spring-cloud-stream is on the classpath, and can be switched off with
 * <code>turbine.stream.enabled</code>.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(EnableBinding.class)
@ConditionalOnProperty(value = "turbine.stream.enabled", matchIfMissing = true)
@EnableBinding(TurbineStreamClient.class)
public class TurbineStreamAutoConfiguration {

	@Autowired
	private BindingServiceProperties bindings;

	@Autowired
	private TurbineStreamProperties properties;

	@PostConstruct
	public void init() {
		BindingProperties inputBinding = this.bindings.getBindings()
				.get(TurbineStreamClient.INPUT);
		if (inputBinding == null) {
			this.bindings.getBindings().put(TurbineStreamClient.INPUT,
					new BindingProperties());
		}
		BindingProperties input = this.bindings.getBindings()
				.get(TurbineStreamClient.INPUT);
		if (input.getDestination() == null) {
			input.setDestination(this.properties.getDestination());
		}
		if (input.getContentType() == null) {
			input.setContentType(this.properties.getContentType());
		}
	}

	@Bean
	public HystrixStreamAggregator hystrixStreamAggregator(ObjectMapper mapper,
			PublishSubject<Map<String, Object>> publisher) {
		return new HystrixStreamAggregator(mapper, publisher);
	}

}
