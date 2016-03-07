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

import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class PreDecorationFilter extends ZuulFilter {

	private RouteLocator routeLocator;

	private boolean addProxyHeaders;
	
	private String dispatcherServletPath;
	private ZuulProperties zuulProperties;	

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	public PreDecorationFilter(RouteLocator routeLocator, boolean addProxyHeaders,
			boolean removeSemicolonContent, String dispatcherServletPath, ZuulProperties zuulProperties) {
		this.routeLocator = routeLocator;
		this.addProxyHeaders = addProxyHeaders;
		this.dispatcherServletPath = dispatcherServletPath;
		this.zuulProperties = zuulProperties;
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
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
		RequestContext ctx = RequestContext.getCurrentContext();
		return !ctx.containsKey("forward.to") // another filter has already forwarded
				&& !ctx.containsKey("serviceId"); // another filter has already determined
													// serviceId
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		final String requestURI = this.urlPathHelper
				.getPathWithinApplication(ctx.getRequest());
		Route route = this.routeLocator.getMatchingRoute(requestURI);
		if (route != null) {
			String location = route.getLocation();
			if (location != null) {
				ctx.put("requestURI", route.getPath());
				ctx.put("proxy", route.getId());
				ctx.put("ignoredHeaders", route.getSensitiveHeaders());

				if (route.getRetryable() != null) {
					ctx.put("retryable", route.getRetryable());
				}

				if (location.startsWith("http:") || location.startsWith("https:")) {
					ctx.setRouteHost(getUrl(location));
					ctx.addOriginResponseHeader("X-Zuul-Service", location);
				}
				else if (location.startsWith("forward:")) {
					ctx.set("forward.to", StringUtils.cleanPath(
							location.substring("forward:".length()) + route.getPath()));
					ctx.setRouteHost(null);
					return null;
				}
				else {
					// set serviceId for use in filters.route.RibbonRequest
					ctx.set("serviceId", location);
					ctx.setRouteHost(null);
					ctx.addOriginResponseHeader("X-Zuul-ServiceId", location);
				}
				if (this.addProxyHeaders) {
					ctx.addZuulRequestHeader("X-Forwarded-Host",
							ctx.getRequest().getServerName());
					ctx.addZuulRequestHeader("X-Forwarded-Port",
							String.valueOf(ctx.getRequest().getServerPort()));
					ctx.addZuulRequestHeader(ZuulHeaders.X_FORWARDED_PROTO,
							ctx.getRequest().getScheme());
					if (StringUtils.hasText(route.getPrefix())) {
						ctx.addZuulRequestHeader("X-Forwarded-Prefix", route.getPrefix());
					}
				}
			}
		}
		else {
			log.warn("No route found for uri: " + requestURI);
			
			String fallBackUri = requestURI;
			String fallbackPrefix = dispatcherServletPath; //default fallback servlet is DispatcherServlet
			
			if (RequestUtils.isZuulServletRequest()) {
				//remove the Zuul servletPath from the requestUri
				log.debug("zuulProperties.getServletPath()=" + zuulProperties.getServletPath());
				fallBackUri = fallBackUri.replaceFirst(zuulProperties.getServletPath(), "");
				log.debug("Replaced Zuul servlet path:" + fallBackUri);
			} else {
				//remove the DispatcherServlet servletPath from the requestUri
				log.debug("dispatcherServletPath=" + dispatcherServletPath);
				fallBackUri = fallBackUri.replaceFirst(dispatcherServletPath, "");
				log.debug("Replaced DispatcherServlet servlet path:" + fallBackUri);
			}
			if (!fallBackUri.startsWith("/")) {
				fallBackUri = "/" + fallBackUri;
			}
			String forwardURI = fallbackPrefix + fallBackUri;
			forwardURI = forwardURI.replaceAll("//", "/");
			ctx.set("forward.to", forwardURI);
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
