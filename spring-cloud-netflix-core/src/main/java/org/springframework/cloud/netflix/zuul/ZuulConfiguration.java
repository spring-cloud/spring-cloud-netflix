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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.FormBodyWrapperFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.Servlet30WrapperFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SendForwardFilter;
import org.springframework.cloud.netflix.zuul.web.ZuulController;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.http.ZuulServlet;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties(ZuulProperties.class)
@ConditionalOnClass(ZuulServlet.class)
public class ZuulConfiguration {

	@Autowired
	private ZuulProperties zuulProperties;

	@Autowired
	private ServerProperties server;

	@Autowired(required = false)
	private ErrorController errorController;

	@Bean
	public HasFeatures zuulFeature() {
		return HasFeatures.namedFeature("Zuul (Simple)", ZuulConfiguration.class);
	}

	@Bean
	@ConditionalOnMissingBean
	public RouteLocator routeLocator() {
		return new SimpleRouteLocator(this.server.getServletPrefix(),
				this.zuulProperties);
	}

	@Bean
	public ZuulController zuulController() {
		return new ZuulController();
	}

	@Bean
	public ZuulHandlerMapping zuulHandlerMapping(RouteLocator routes) {
		ZuulHandlerMapping mapping = new ZuulHandlerMapping(routes, zuulController());
		mapping.setErrorController(this.errorController);
		return mapping;
	}

	@Bean
	public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
		return new ZuulRefreshListener();
	}

	@Bean
	public ServletRegistrationBean zuulServlet() {
		return new ServletRegistrationBean(new ZuulServlet(),
				this.zuulProperties.getServletPattern());
	}

	// pre filters

	@Bean
	public FormBodyWrapperFilter formBodyWrapperFilter() {
		return new FormBodyWrapperFilter();
	}

	@Bean
	public DebugFilter debugFilter() {
		return new DebugFilter();
	}

	@Bean
	public Servlet30WrapperFilter servlet30WrapperFilter() {
		return new Servlet30WrapperFilter();
	}

	// post filters

	@Bean
	public SendResponseFilter sendResponseFilter() {
		return new SendResponseFilter();
	}

	@Bean
	public SendErrorFilter sendErrorFilter() {
		return new SendErrorFilter();
	}

	@Bean
	public SendForwardFilter sendForwardFilter() {
		return new SendForwardFilter();
	}

	@Configuration
	protected static class ZuulFilterConfiguration {

		@Autowired
		private Map<String, ZuulFilter> filters;

		@Bean
		public ZuulFilterInitializer zuulFilterInitializer() {
			return new ZuulFilterInitializer(this.filters);
		}

	}

	private static class ZuulRefreshListener
			implements ApplicationListener<ApplicationEvent> {

		@Autowired
		private ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent
					|| event instanceof RefreshScopeRefreshedEvent
					|| event instanceof RoutesRefreshedEvent) {
				this.zuulHandlerMapping.setDirty(true);
			}
		}

	}

}
