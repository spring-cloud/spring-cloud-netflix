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

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.DiscoveryHeartbeatEvent;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
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

	@Bean
	@Override
	public ProxyRouteLocator routeLocator() {
		return new ProxyRouteLocator(this.discovery, this.zuulProperties);
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	protected static class RoutesEndpointConfuguration {
		@Autowired
		private ProxyRouteLocator routeLocator;

		@Bean
		// @RefreshScope
		public RoutesEndpoint zuulEndpoint() {
			return new RoutesEndpoint(this.routeLocator);
		}
	}

	// pre filters
	@Bean
	public PreDecorationFilter preDecorationFilter() {
		return new PreDecorationFilter(routeLocator(), this.zuulProperties);
	}

	// route filters
	@Bean
	public RibbonRoutingFilter ribbonRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (this.traces != null) {
			helper.setTraces(this.traces);
		}
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper, this.clientFactory);
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

	private static class ZuulRefreshListener implements
			ApplicationListener<ApplicationEvent> {

		private AtomicReference<Object> latestHeartbeat = new AtomicReference<>();

		@Autowired
		private ProxyRouteLocator routeLocator;

		@Autowired
		ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof InstanceRegisteredEvent
					|| event instanceof RefreshScopeRefreshedEvent
					|| event instanceof RoutesRefreshedEvent) {
				reset();
			}
			else if (event instanceof DiscoveryHeartbeatEvent) {
				DiscoveryHeartbeatEvent e = (DiscoveryHeartbeatEvent) event;
				if (this.latestHeartbeat.get() == null
						|| !this.latestHeartbeat.get().equals(e.getValue())) {
					this.latestHeartbeat.set(e.getValue());
					reset();
				}
			}

		}

		private void reset() {
			this.routeLocator.resetRoutes();
			this.zuulHandlerMapping.registerHandlers();
		}

	}

}
