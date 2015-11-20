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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.HttpEncodingProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.DefaultEurekaServerContext;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.DefaultServerCodecs;
import com.netflix.eureka.resources.ServerCodecs;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * @author Gunnar Hillert
 */
@Configuration
@Import(EurekaServerInitializerConfiguration.class)
@EnableDiscoveryClient
@EnableConfigurationProperties(EurekaDashboardProperties.class)
public class EurekaServerConfiguration extends WebMvcConfigurerAdapter {
	/**
	 * List of packages containing Jersey resources required by the Eureka server
	 */
	private static String[] EUREKA_PACKAGES = new String[] {
		"com.netflix.discovery",
		"com.netflix.eureka"};

	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Autowired
	private EurekaServerConfig eurekaServerConfig;

	@Autowired
	private EurekaClientConfig eurekaClientConfig;

	@Autowired
	private EurekaClient eurekaClient;

	/*
	 * Setting expectedNumberOfRenewsPerMin to non-zero to ensure that even an isolated
	 * server can adjust its eviction policy to the number of registrations (when it's
	 * zero, even a successful registration won't reset the rate threshold in
	 * InstanceRegistry.register()).
	 */
	@Value("${eureka.server.expectedNumberOfRenewsPerMin:1}")
	private int expectedNumberOfRenewsPerMin;

	@Value("${eureka.server.defaultOpenForTrafficCount:1}")
	private int defaultOpenForTrafficCount;

	@Bean
	public HasFeatures eurekaServerFeature() {
		return HasFeatures.namedFeature("Eureka Server", EurekaServerConfiguration.class);
	}

	@Bean
	@ConditionalOnMissingBean
	public EurekaServerConfig eurekaServerConfig() {
		return new EurekaServerConfigBean();
	}

	//TODO: is there a better way?
	@Bean(name = "spring.http.encoding.CONFIGURATION_PROPERTIES")
	public HttpEncodingProperties httpEncodingProperties() {
		HttpEncodingProperties properties = new HttpEncodingProperties();
		properties.setForce(false);
		return properties;
	}

	@Bean
	@ConditionalOnProperty(prefix = "eureka.dashboard", name = "enabled", matchIfMissing = true)
	public EurekaController eurekaController() {
		return new EurekaController(applicationInfoManager);
	}

	@Bean
	public ServerCodecs serverCodecs() {
		return new DefaultServerCodecs(eurekaServerConfig);
	}

	@Bean
	public PeerAwareInstanceRegistry peerAwareInstanceRegistry(ServerCodecs serverCodecs) {
		eurekaClient.getApplications(); // force initialization
		return new InstanceRegistry(eurekaServerConfig, eurekaClientConfig, serverCodecs,
				eurekaClient, expectedNumberOfRenewsPerMin, defaultOpenForTrafficCount);
	}

	@Bean
	public PeerEurekaNodes peerEurekaNodes(PeerAwareInstanceRegistry registry,
										   ServerCodecs serverCodecs) {
		return new PeerEurekaNodes(registry, eurekaServerConfig, eurekaClientConfig,
				serverCodecs, applicationInfoManager);
	}

	@Bean
	public EurekaServerContext eurekaServerContext(ServerCodecs serverCodecs,
												   PeerAwareInstanceRegistry registry,
												   PeerEurekaNodes peerEurekaNodes) {
		return new DefaultEurekaServerContext(eurekaServerConfig, serverCodecs, registry,
				peerEurekaNodes, applicationInfoManager);
	}

	@Bean
	public EurekaServerBootstrap eurekaServerBootstrap(PeerAwareInstanceRegistry registry,
													   EurekaServerContext serverContext) {
		return new EurekaServerBootstrap(applicationInfoManager, eurekaClientConfig,
				eurekaServerConfig, registry, serverContext);
	}

	/**
	 * Register the Jersey filter
	 *
	 * @param eurekaJerseyApp the jersey application
	 * @return
	 */
	@Bean
	public FilterRegistrationBean jerseyFilterRegistration(
			javax.ws.rs.core.Application eurekaJerseyApp) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(new ServletContainer(eurekaJerseyApp));
		bean.setOrder(Ordered.LOWEST_PRECEDENCE);
		bean.setUrlPatterns(Collections
				.singletonList(EurekaServerConfigBean.DEFAULT_PREFIX + "/*"));

		return bean;
	}

	/**
	 * Construct a Jersey {@link javax.ws.rs.core.Application} with all the resources
	 * required by the Eureka server.
	 */
	@Bean
	public javax.ws.rs.core.Application jerseyApplication(Environment environment,
			ResourceLoader resourceLoader) {

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
				false, environment);

		// Filter to include only classes that have a particular annotation.
		//
		provider.addIncludeFilter(new AnnotationTypeFilter(Path.class));
		provider.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

		// Find classes in Eureka packages (or subpackages)
		//
		Set<Class<?>> classes = new HashSet<Class<?>>();
		for (String basePackage : EUREKA_PACKAGES) {
			Set<BeanDefinition> beans = provider.findCandidateComponents(basePackage);
			for (BeanDefinition bd : beans) {
				Class<?> cls = ClassUtils.resolveClassName(bd.getBeanClassName(),
						resourceLoader.getClassLoader());
				classes.add(cls);
			}
		}

		// Construct the Jersey ResourceConfig
		//
		Map<String, Object> propsAndFeatures = new HashMap<String, Object>();
		propsAndFeatures.put(
				// Skip static content used by the webapp
				ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX,
				EurekaServerConfigBean.DEFAULT_PREFIX + "/(fonts|images|css|js)/.*");

		DefaultResourceConfig rc = new DefaultResourceConfig(classes);
		rc.setPropertiesAndFeatures(propsAndFeatures);

		return rc;
	}

	@Bean
	public FilterRegistrationBean traceFilterRegistration(
			@Qualifier("webRequestLoggingFilter") Filter filter) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(filter);
		bean.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return bean;
	}
}
