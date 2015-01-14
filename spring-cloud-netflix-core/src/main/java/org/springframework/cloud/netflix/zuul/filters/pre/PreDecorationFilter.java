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

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.ProxyRouteLocator.ProxyRouteSpec;
import org.springframework.cloud.netflix.zuul.ZuulProperties;
import org.springframework.util.StringUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PreDecorationFilter extends ZuulFilter {
	private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

	private ProxyRouteLocator routeLocator;

	private ZuulProperties properties;

	public PreDecorationFilter(ProxyRouteLocator routeLocator, ZuulProperties properties) {
		this.routeLocator = routeLocator;
		this.properties = properties;
	}

	@Override
	public int filterOrder() {
		return 5;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();

		final String requestURI = ctx.getRequest().getRequestURI();

		ProxyRouteSpec route = this.routeLocator.getMatchingRoute(requestURI);

		if (route != null) {

			String location = route.getLocation();

			if (location != null) {

				ctx.put("requestURI", route.getPath());
				ctx.put("proxy", route.getId());

				if (location.startsWith("http:") || location.startsWith("https:")) {
					ctx.setRouteHost(getUrl(location));
					ctx.addOriginResponseHeader("X-Zuul-Service", location);
				}
				else {
					// set serviceId for use in filters.route.RibbonRequest
					ctx.set("serviceId", location);
					ctx.setRouteHost(null);
					ctx.addOriginResponseHeader("X-Zuul-ServiceId", location);
				}

				if (this.properties.isAddProxyHeaders()) {
					ctx.addZuulRequestHeader(
							"X-Forwarded-Host",
							ctx.getRequest().getServerName() + ":"
									+ String.valueOf(ctx.getRequest().getServerPort()));
					if (StringUtils.hasText(route.getPrefix())) {
						ctx.addZuulRequestHeader("X-Forwarded-Prefix", route.getPrefix());
					}
				}
			}
		}
		else {
			LOG.warn("No route found for uri: " + requestURI);
			ctx.set("error.status_code", HttpServletResponse.SC_NOT_FOUND);
		}
		return null;
	}

	private URL getUrl(String target) {
		try {
			return new URL(target);
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Target URL is malformed", e);
		}
	}
}
