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

package org.springframework.cloud.netflix.zuul;

import java.util.Collections;
import java.util.List;

import com.netflix.zuul.filters.FilterRegistry;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.TraceProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Biju Kunjummen
 */
@Configuration
@Import({ RibbonCommandFactoryConfiguration.RestClientRibbonConfiguration.class,
		RibbonCommandFactoryConfiguration.OkHttpRibbonConfiguration.class,
		RibbonCommandFactoryConfiguration.HttpClientRibbonConfiguration.class,
		HttpClientConfiguration.class })
@ConditionalOnBean(ZuulProxyMarkerConfiguration.Marker.class)
public class ZuulProxyAutoConfiguration extends ZuulServerAutoConfiguration {

	@SuppressWarnings("rawtypes")
	@Autowired(required = false)
	private List<RibbonRequestCustomizer> requestCustomizers = Collections.emptyList();

	@Autowired(required = false)
	private Registration registration;

	@Autowired
	private DiscoveryClient discovery;

	@Autowired
	private ServiceRouteMapper serviceRouteMapper;

	@Override
	public HasFeatures zuulFeature() {
		return HasFeatures.namedFeature("Zuul (Discovery)",
				ZuulProxyAutoConfiguration.class);
	}

	@Bean
	@ConditionalOnMissingBean(DiscoveryClientRouteLocator.class)
	public DiscoveryClientRouteLocator discoveryRouteLocator() {
		return new DiscoveryClientRouteLocator(this.server.getServlet().getContextPath(),
				this.discovery, this.zuulProperties, this.serviceRouteMapper,
				this.registration);
	}

	// pre filters
	@Bean
	@ConditionalOnMissingBean(PreDecorationFilter.class)
	public PreDecorationFilter preDecorationFilter(RouteLocator routeLocator,
			ProxyRequestHelper proxyRequestHelper) {
		return new PreDecorationFilter(routeLocator,
				this.server.getServlet().getContextPath(), this.zuulProperties,
				proxyRequestHelper);
	}

	// route filters
	@Bean
	@ConditionalOnMissingBean(RibbonRoutingFilter.class)
	public RibbonRoutingFilter ribbonRoutingFilter(ProxyRequestHelper helper,
			RibbonCommandFactory<?> ribbonCommandFactory) {
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper, ribbonCommandFactory,
				this.requestCustomizers);
		return filter;
	}

	@Bean
	@ConditionalOnMissingBean({ SimpleHostRoutingFilter.class,
			CloseableHttpClient.class })
	public SimpleHostRoutingFilter simpleHostRoutingFilter(ProxyRequestHelper helper,
			ZuulProperties zuulProperties,
			ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
			ApacheHttpClientFactory httpClientFactory) {
		return new SimpleHostRoutingFilter(helper, zuulProperties,
				connectionManagerFactory, httpClientFactory);
	}

	@Bean
	@ConditionalOnMissingBean({ SimpleHostRoutingFilter.class })
	public SimpleHostRoutingFilter simpleHostRoutingFilter2(ProxyRequestHelper helper,
			ZuulProperties zuulProperties, CloseableHttpClient httpClient) {
		return new SimpleHostRoutingFilter(helper, zuulProperties, httpClient);
	}

	@Bean
	@ConditionalOnMissingBean(ServiceRouteMapper.class)
	public ServiceRouteMapper serviceRouteMapper() {
		return new SimpleServiceRouteMapper();
	}

	@Configuration
	@ConditionalOnMissingClass("org.springframework.boot.actuate.health.Health")
	protected static class NoActuatorConfiguration {

		@Bean
		public ProxyRequestHelper proxyRequestHelper(ZuulProperties zuulProperties) {
			ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);
			return helper;
		}

	}

	@Configuration
	@ConditionalOnClass(Health.class)
	protected static class EndpointConfiguration {

		@Autowired(required = false)
		private HttpTraceRepository traces;

		@Bean
		@ConditionalOnEnabledEndpoint
		public RoutesEndpoint routesEndpoint(RouteLocator routeLocator) {
			return new RoutesEndpoint(routeLocator);
		}

		@ConditionalOnEnabledEndpoint
		@Bean
		public FiltersEndpoint filtersEndpoint() {
			FilterRegistry filterRegistry = FilterRegistry.instance();
			return new FiltersEndpoint(filterRegistry);
		}

		@Bean
		public ProxyRequestHelper proxyRequestHelper(ZuulProperties zuulProperties) {
			TraceProxyRequestHelper helper = new TraceProxyRequestHelper(zuulProperties);
			if (this.traces != null) {
				helper.setTraces(this.traces);
			}
			return helper;
		}

	}

}
