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

package org.springframework.cloud.netflix.zuul;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.ServletDetectionFilter;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
public class ZuulProxyConfiguration extends ZuulConfiguration {

	@Autowired(required = false)
	private TraceRepository traces;

	@Autowired
	private SpringClientFactory clientFactory;

	@Autowired
	private DiscoveryClient discovery;

	@Autowired
	private ServiceRouteMapper serviceRouteMapper;

	@Override
	public HasFeatures zuulFeature() {
		return HasFeatures.namedFeature("Zuul (Discovery)", ZuulProxyConfiguration.class);
	}

	@Bean
	@Override
	@ConditionalOnMissingBean(RouteLocator.class)
	public DiscoveryClientRouteLocator routeLocator() {
		return new DiscoveryClientRouteLocator(this.server.getServletPrefix(),
				this.discovery, this.zuulProperties, this.serviceRouteMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	public RibbonCommandFactory<?> ribbonCommandFactory() {
		return new RestClientRibbonCommandFactory(this.clientFactory);
	}

	// pre filters
	@Bean
	public ServletDetectionFilter servletDetectionFilter() {
		return new ServletDetectionFilter();
	}

	@Bean
	public PreDecorationFilter preDecorationFilter(RouteLocator routeLocator) {
		return new PreDecorationFilter(routeLocator,
				this.zuulProperties.isAddProxyHeaders());
	}

	// route filters
	@Bean
	public RibbonRoutingFilter ribbonRoutingFilter(ProxyRequestHelper helper,
			RibbonCommandFactory<?> ribbonCommandFactory) {
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper,
				ribbonCommandFactory);
		return filter;
	}

	@Bean
	public SimpleHostRoutingFilter simpleHostRoutingFilter(ProxyRequestHelper helper) {
		return new SimpleHostRoutingFilter(helper);
	}

	@Bean
	public ProxyRequestHelper proxyRequestHelper() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (this.traces != null) {
			helper.setTraces(this.traces);
		}
		helper.setIgnoredHeaders(this.zuulProperties.getIgnoredHeaders());
		return helper;
	}

	@Bean
	public ApplicationListener<ApplicationEvent> zuulDiscoveryRefreshRoutesListener() {
		return new ZuulDiscoveryRefreshListener();
	}

	@Bean
	@ConditionalOnMissingBean(ServiceRouteMapper.class)
	public ServiceRouteMapper serviceRouteMapper() {
		return new SimpleServiceRouteMapper();
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	protected static class RoutesEndpointConfiguration {

		@Bean
		public RoutesEndpoint zuulEndpoint(DiscoveryClientRouteLocator routeLocator) {
			return new RoutesEndpoint(routeLocator);
		}

	}

	private static class ZuulDiscoveryRefreshListener
			implements ApplicationListener<ApplicationEvent> {

		private HeartbeatMonitor monitor = new HeartbeatMonitor();

		@Autowired
		private ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof InstanceRegisteredEvent) {
				reset();
			}
			else if (event instanceof ParentHeartbeatEvent) {
				ParentHeartbeatEvent e = (ParentHeartbeatEvent) event;
				resetIfNeeded(e.getValue());
			}
			else if (event instanceof HeartbeatEvent) {
				HeartbeatEvent e = (HeartbeatEvent) event;
				resetIfNeeded(e.getValue());
			}

		}

		private void resetIfNeeded(Object value) {
			if (this.monitor.update(value)) {
				reset();
			}
		}

		private void reset() {
			this.zuulHandlerMapping.setDirty(true);
		}

	}

}
