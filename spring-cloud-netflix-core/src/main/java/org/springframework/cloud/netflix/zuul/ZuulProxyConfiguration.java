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
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
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
	private ZuulProperties zuulProperties;

	@Autowired
	private ServerProperties server;

	@Bean
	@Override
	public ProxyRouteLocator routeLocator() {
		return new ProxyRouteLocator(this.server.getServletPrefix(), this.discovery,
				this.zuulProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public RibbonCommandFactory ribbonCommandFactory() {
		return new RestClientRibbonCommandFactory(this.clientFactory);
	}

	// pre filters
	@Bean
	public PreDecorationFilter preDecorationFilter() {
		return new PreDecorationFilter(routeLocator(),
				this.zuulProperties.isAddProxyHeaders());
	}

	// route filters
	@Bean
	public RibbonRoutingFilter ribbonRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (this.traces != null) {
			helper.setTraces(this.traces);
		}
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper, ribbonCommandFactory());
		return filter;
	}

	@Bean
	public SimpleHostRoutingFilter simpleHostRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (this.traces != null) {
			helper.setTraces(this.traces);
		}
		return new SimpleHostRoutingFilter(helper);
	}

	@Bean
	@Override
	public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
		return new ZuulRefreshListener();
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	protected static class RoutesEndpointConfiguration {

		@Autowired
		private ProxyRouteLocator routeLocator;

		@Bean
		// @RefreshScope
		public RoutesEndpoint zuulEndpoint() {
			return new RoutesEndpoint(this.routeLocator);
		}

	}

	private static class ZuulRefreshListener implements
			ApplicationListener<ApplicationEvent> {

		private HeartbeatMonitor monitor = new HeartbeatMonitor();

		@Autowired
		private ProxyRouteLocator routeLocator;

		@Autowired
		private ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof InstanceRegisteredEvent
					|| event instanceof RefreshScopeRefreshedEvent
					|| event instanceof RoutesRefreshedEvent) {
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
			this.routeLocator.resetRoutes();
			this.zuulHandlerMapping.registerHandlers();
		}

	}

}
