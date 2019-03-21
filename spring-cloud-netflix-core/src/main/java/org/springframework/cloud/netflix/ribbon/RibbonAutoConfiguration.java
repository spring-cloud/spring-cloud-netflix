/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.netflix.client.IClient;
import com.netflix.client.http.HttpRequest;
import com.netflix.ribbon.Ribbon;

/**
 * Auto configuration for Ribbon (client side load balancing).
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ IClient.class, RestTemplate.class, Ribbon.class})
@RibbonClients
@AutoConfigureAfter(name = "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration")
@AutoConfigureBefore(LoadBalancerAutoConfiguration.class)
public class RibbonAutoConfiguration {

	@Autowired(required = false)
	private List<RibbonClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures ribbonFeature() {
		return HasFeatures.namedFeature("Ribbon", Ribbon.class);
	}

	@Bean
	public SpringClientFactory springClientFactory() {
		SpringClientFactory factory = new SpringClientFactory();
		factory.setConfigurations(this.configurations);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(LoadBalancerClient.class)
	public LoadBalancerClient loadBalancerClient() {
		return new RibbonLoadBalancerClient(springClientFactory());
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory(SpringClientFactory clientFactory) {
		return new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
	}

	@Bean
	@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
	public LoadBalancedRetryPolicyFactory neverRetryPolicyFactory() {
		return new LoadBalancedRetryPolicyFactory.NeverRetryFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	public PropertiesFactory propertiesFactory() {
		return new PropertiesFactory();
	}

	@Configuration
	@ConditionalOnClass(HttpRequest.class)
	@ConditionalOnRibbonRestClient
	protected static class RibbonClientConfig {

		@Autowired
		private SpringClientFactory springClientFactory;

		@Bean
		public RestTemplateCustomizer restTemplateCustomizer(
				final RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory) {
			return new RestTemplateCustomizer() {
				@Override
				public void customize(RestTemplate restTemplate) {
					restTemplate.setRequestFactory(ribbonClientHttpRequestFactory);
				}
			};
		}

		@Bean
		public RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory() {
			return new RibbonClientHttpRequestFactory(this.springClientFactory);
		}
	}

	//TODO: support for autoconfiguring restemplate to use apache http client or okhttp

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnRibbonRestClientCondition.class)
	@interface ConditionalOnRibbonRestClient { }

	private static class OnRibbonRestClientCondition extends AnyNestedCondition {
		public OnRibbonRestClientCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Deprecated //remove in Edgware"
		@ConditionalOnProperty("ribbon.http.client.enabled")
		static class ZuulProperty {}

		@ConditionalOnProperty("ribbon.restclient.enabled")
		static class RibbonProperty {}
	}
}
