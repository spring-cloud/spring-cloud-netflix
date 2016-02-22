/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@RestController
@EnableEurekaClient
public class EurekaSampleApplication {

	@Autowired
	DiscoveryClient discoveryClient;

	@Bean
	public InMemoryMetricRepository inMemoryMetricRepository() {
		return new InMemoryMetricRepository();
	}

	@Bean
	public HealthCheckHandler healthCheckHandler() {
		return new HealthCheckHandler() {
			@Override
			public InstanceInfo.InstanceStatus getStatus(
					InstanceInfo.InstanceStatus currentStatus) {
				return InstanceInfo.InstanceStatus.UP;
			}
		};
	}

	@RequestMapping("/")
	public String home() {
		return "Hello world "+discoveryClient.getLocalServiceInstance().getUri();
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(EurekaSampleApplication.class).web(true).run(args);
	}

}
