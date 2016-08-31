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

package org.springframework.cloud.netflix.feign;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Client;
import feign.Feign;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;

/**
 * @author Spencer Gibb
 * @author Julien Roy
 */
@Configuration
@ConditionalOnClass(Feign.class)
public class FeignAutoConfiguration {

	@Autowired(required = false)
	private List<FeignClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures feignFeature() {
		return HasFeatures.namedFeature("Feign", Feign.class);
	}

	@Bean
	public FeignContext feignContext() {
		FeignContext context = new FeignContext();
		context.setConfigurations(this.configurations);
		return context;
	}

	@Configuration
	@ConditionalOnClass(name = "feign.hystrix.HystrixFeign")
	protected static class HystrixFeignTargeterConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			return new HystrixTargeter();
		}
	}

	@Configuration
	@ConditionalOnMissingClass("feign.hystrix.HystrixFeign")
	protected static class DefaultFeignTargeterConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			return new DefaultTargeter();
		}
	}

	// the following configuration is for alternate feign clients if
	// ribbon is not on the class path.
	// see corresponding configurations in FeignRibbonClientAutoConfiguration
	// for load balanced ribbon clients.
	@Configuration
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
	@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
	protected static class HttpClientFeignConfiguration {

		@Autowired(required = false)
		private HttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient() {
			if (this.httpClient != null) {
				return new ApacheHttpClient(this.httpClient);
			}
			return new ApacheHttpClient();
		}
	}

	@Configuration
	@ConditionalOnClass(OkHttpClient.class)
	@ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
	@ConditionalOnProperty(value = "feign.okhttp.enabled", matchIfMissing = true)
	protected static class OkHttpFeignConfiguration {

		@Autowired(required = false)
		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient() {
			if (this.okHttpClient != null) {
				return new OkHttpClient(this.okHttpClient);
			}
			return new OkHttpClient();
		}
	}

}
