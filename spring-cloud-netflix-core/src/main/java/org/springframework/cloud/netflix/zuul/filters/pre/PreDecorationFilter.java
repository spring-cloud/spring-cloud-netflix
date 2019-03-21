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

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator.ProxyRouteSpec;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

@CommonsLog
public class PreDecorationFilter extends ZuulFilter {

	private ProxyRouteLocator routeLocator;

	private boolean addProxyHeaders;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	public PreDecorationFilter(ProxyRouteLocator routeLocator, boolean addProxyHeaders) {
		this.routeLocator = routeLocator;
		this.addProxyHeaders = addProxyHeaders;
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
		final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx
				.getRequest());
		ProxyRouteSpec route = this.routeLocator.getMatchingRoute(requestURI);
		if (route != null) {
			String location = route.getLocation();
			if (location != null) {
				ctx.put("requestURI", route.getPath());
				ctx.put("proxy", route.getId());

				if (route.getRetryable() != null) {
					ctx.put("retryable", route.getRetryable());
				}

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
				if (this.addProxyHeaders) {
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
			log.warn("No route found for uri: " + requestURI);
			ctx.set("error.status_code", HttpServletResponse.SC_NOT_FOUND);
		}
		return null;
	}

	private URL getUrl(String target) {
		try {
			return new URL(target);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Target URL is malformed", ex);
		}
	}
}
