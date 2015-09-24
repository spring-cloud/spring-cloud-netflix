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

import static org.springframework.cloud.util.IdUtils.getDefaultInstanceId;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@AutoConfigureBefore({ NoopDiscoveryClientAutoConfiguration.class,
		CommonsClientAutoConfiguration.class })
@AutoConfigureAfter(RefreshAutoConfiguration.class)
public class EurekaClientAutoConfiguration {

	@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
	int nonSecurePort;

	@Autowired
	ConfigurableEnvironment env;

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class, search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search = SearchStrategy.CURRENT)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.setNonSecurePort(this.nonSecurePort);
		instance.setSid(getDefaultInstanceId(env));
		return instance;
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationInfoManager.class)
	public ApplicationInfoManager applicationInfoManager(EurekaInstanceConfig config,
			InstanceInfo instanceInfo) {
		return new ApplicationInfoManager(config, instanceInfo);
	}

	@Bean
	@ConditionalOnMissingBean(InstanceInfo.class)
	public InstanceInfo instanceInfo(EurekaInstanceConfig config) {
		return new InstanceInfoFactory().create(config);
	}

	@Bean
	public DiscoveryClient discoveryClient(EurekaInstanceConfig config,
			EurekaClient client) {
		return new EurekaDiscoveryClient(config, client);
	}

	@Configuration
	@ConditionalOnMissingRefreshScope
	protected static class EurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		@SneakyThrows
		public EurekaClient eurekaClient(ApplicationInfoManager applicationInfoManager,
				EurekaClientConfig config, EurekaInstanceConfig instance) {
			applicationInfoManager.initComponent(instance);
			return new CloudEurekaClient(applicationInfoManager, config, this.context);
		}
	}

	@Configuration
	@ConditionalOnRefreshScope
	protected static class RefreshableEurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		@SneakyThrows
		@org.springframework.cloud.context.config.annotation.RefreshScope
		public EurekaClient eurekaClient(ApplicationInfoManager applicationInfoManager,
				EurekaClientConfig config, EurekaInstanceConfig instance) {
			applicationInfoManager.initComponent(instance);
			return new CloudEurekaClient(applicationInfoManager, config, this.context);
		}

	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnMissingRefreshScopeCondition.class)
	@interface ConditionalOnMissingRefreshScope {

	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnRefreshScopeCondition.class)
	@interface ConditionalOnRefreshScope {

	}

	private static class OnMissingRefreshScopeCondition extends AnyNestedCondition {

		public OnMissingRefreshScopeCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingClass("org.springframework.cloud.context.scope.refresh.RefreshScope")
		static class MissingClass {
		}

		@ConditionalOnMissingBean(RefreshScope.class)
		static class MissingScope {
		}

	}

	private static class OnRefreshScopeCondition extends AllNestedConditions {

		public OnRefreshScopeCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(RefreshScope.class)
		@ConditionalOnBean(RefreshScope.class)
		static class FoundScope {
		}

	}
}
