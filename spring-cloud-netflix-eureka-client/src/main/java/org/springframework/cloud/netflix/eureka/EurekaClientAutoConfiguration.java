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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

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
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient.DiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import static org.springframework.cloud.util.IdUtils.getDefaultInstanceId;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
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

	@Value("${management.port:${MANAGEMENT_PORT:${server.port:${SERVER_PORT:${PORT:8080}}}}}")
	int managementPort;

	@Value("${eureka.instance.hostname:${EUREKA_INSTANCE_HOSTNAME:}}")
	String hostname;

	@Autowired
	ConfigurableEnvironment env;

	@Bean
	public HasFeatures eurekaFeature() {
		return HasFeatures.namedFeature("Eureka Client", EurekaClient.class);
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class, search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		if ("bootstrap".equals(this.env.getProperty("spring.config.name"))) {
			// We don't register during bootstrap by default, but there will be another
			// chance later.
			client.setRegisterWithEureka(false);
		}
		return client;
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search = SearchStrategy.CURRENT)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils) {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);
		instance.setNonSecurePort(this.nonSecurePort);
		instance.setInstanceId(getDefaultInstanceId(this.env));
		if (this.managementPort != this.nonSecurePort && this.managementPort != 0) {
			if (StringUtils.hasText(this.hostname)) {
				instance.setHostname(this.hostname);
			}
			String scheme = instance.getSecurePortEnabled() ? "https" : "http";
			instance.setStatusPageUrl(scheme + "://" + instance.getHostname() + ":"
					+ this.managementPort + instance.getStatusPageUrlPath());
			instance.setHealthCheckUrl(scheme + "://" + instance.getHostname() + ":"
					+ this.managementPort + instance.getHealthCheckUrlPath());
		}
		return instance;
	}

	@Bean
	public DiscoveryClient discoveryClient(EurekaInstanceConfig config,
			EurekaClient client) {
		return new EurekaDiscoveryClient(config, client);
	}

	@Bean
	@ConditionalOnMissingBean(value = DiscoveryClientOptionalArgs.class, search = SearchStrategy.CURRENT)
	public MutableDiscoveryClientOptionalArgs discoveryClientOptionalArgs() {
		return new MutableDiscoveryClientOptionalArgs();
	}

	@Configuration
	@ConditionalOnMissingRefreshScope
	protected static class EurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private DiscoveryClientOptionalArgs optionalArgs;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		public EurekaClient eurekaClient(ApplicationInfoManager manager,
				EurekaClientConfig config) {
			DiscoveryClientOptionalArgs args = EurekaClientAutoConfiguration
					.getOptionalArgs(config, this.optionalArgs);
			return new CloudEurekaClient(manager, config, args, this.context);
		}

		@Bean
		@ConditionalOnMissingBean(value = ApplicationInfoManager.class, search = SearchStrategy.CURRENT)
		public ApplicationInfoManager eurekaApplicationInfoManager(
				EurekaInstanceConfig config) {
			InstanceInfo instanceInfo = new InstanceInfoFactory().create(config);
			return new ApplicationInfoManager(config, instanceInfo);
		}
	}

	@Configuration
	@ConditionalOnRefreshScope
	protected static class RefreshableEurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private DiscoveryClientOptionalArgs optionalArgs;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		@org.springframework.cloud.context.config.annotation.RefreshScope
		@Lazy
		public EurekaClient eurekaClient(ApplicationInfoManager manager,
				EurekaClientConfig config, EurekaInstanceConfig instance) {
			manager.getInfo(); // force initialization
			DiscoveryClientOptionalArgs args = EurekaClientAutoConfiguration
					.getOptionalArgs(config, this.optionalArgs);
			return new CloudEurekaClient(manager, config, args, this.context);
		}

		@Bean
		@ConditionalOnMissingBean(value = ApplicationInfoManager.class, search = SearchStrategy.CURRENT)
		@org.springframework.cloud.context.config.annotation.RefreshScope
		@Lazy
		public ApplicationInfoManager eurekaApplicationInfoManager(
				EurekaInstanceConfig config) {
			InstanceInfo instanceInfo = new InstanceInfoFactory().create(config);
			return new ApplicationInfoManager(config, instanceInfo);
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnMissingRefreshScopeCondition.class)
	@interface ConditionalOnMissingRefreshScope {

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
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

		@ConditionalOnMissingBean(RefreshAutoConfiguration.class)
		static class MissingScope {
		}

	}

	private static class OnRefreshScopeCondition extends AllNestedConditions {

		public OnRefreshScopeCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(RefreshScope.class)
		@ConditionalOnBean(RefreshAutoConfiguration.class)
		static class FoundScope {
		}

	}

	public static DiscoveryClientOptionalArgs getOptionalArgs(EurekaClientConfig config,
			DiscoveryClientOptionalArgs optionalArgs) {
		Collection<ClientFilter> filters = new LinkedHashSet<>();
		if (optionalArgs instanceof MutableDiscoveryClientOptionalArgs) {
			MutableDiscoveryClientOptionalArgs mutable = (MutableDiscoveryClientOptionalArgs) optionalArgs;
			filters = mutable.getAdditionalFilters() != null
					? mutable.getAdditionalFilters() : filters;
			ClientFilter filter = getAuthFilter(config);
			if (filter != null) {
				filters.add(filter);
			}
			mutable.setAdditionalFilters(filters);
		}
		return optionalArgs;
	}

	private static ClientFilter getAuthFilter(EurekaClientConfig config) {
		// Netflix throws away the basic auth credentials from the service URL at runtime,
		// so we look at the default zone and try and lift some credentials from there,
		// assuming that they don't change from host to host. If they do change from host
		// to host user will have to create a custom ClientFilter.
		List<String> urls = config
				.getEurekaServerServiceUrls(EurekaClientConfigBean.DEFAULT_ZONE);
		for (String url : urls) {
			try {
				String authority = new URI(url).getAuthority();
				authority = authority != null && authority.contains("@")
						? authority.substring(0, authority.indexOf("@")) : null;
				if (authority != null) {
					String[] values = StringUtils.split(authority, ":");
					if (values == null) {
						values = new String[] { authority, "" };
					}
					return new HTTPBasicAuthFilter(values[0], values[1]);
				}
			}
			catch (URISyntaxException e) {
				// This should not occur
				throw new IllegalArgumentException("Cannot parse service URL: " + url, e);
			}
		}
		return null;
	}

}
