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

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
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

	public static final int FILTER_ORDER = 5;

	private RouteLocator routeLocator;

	private String dispatcherServletPath;

	private ZuulProperties properties;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private ProxyRequestHelper proxyRequestHelper;

	public PreDecorationFilter(RouteLocator routeLocator, String dispatcherServletPath,
			ZuulProperties properties, ProxyRequestHelper proxyRequestHelper) {
		this.routeLocator = routeLocator;
		this.properties = properties;
		this.urlPathHelper
				.setRemoveSemicolonContent(properties.isRemoveSemicolonContent());
		this.dispatcherServletPath = dispatcherServletPath;
		this.proxyRequestHelper = proxyRequestHelper;
	}

	@Override
	public int filterOrder() {
		return FILTER_ORDER;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return !ctx.containsKey("forward.to") // a filter has already forwarded
				&& !ctx.containsKey("serviceId"); // a filter has already determined
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
				if (!route.isCustomSensitiveHeaders()) {
					this.proxyRequestHelper.addIgnoredHeaders(
							this.properties.getSensitiveHeaders().toArray(new String[0]));
				}
				else {
					this.proxyRequestHelper.addIgnoredHeaders(
							route.getSensitiveHeaders().toArray(new String[0]));
				}

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
				if (this.properties.isAddProxyHeaders()) {
					ctx.addZuulRequestHeader("X-Forwarded-Host", toHostHeader(ctx.getRequest()));
					ctx.addZuulRequestHeader("X-Forwarded-Port",
							String.valueOf(ctx.getRequest().getServerPort()));
					ctx.addZuulRequestHeader(ZuulHeaders.X_FORWARDED_PROTO,
							ctx.getRequest().getScheme());
					String forwardedPrefix =
							ctx.getRequest().getHeader("X-Forwarded-Prefix");
					String contextPath = ctx.getRequest().getContextPath();
					String prefix = StringUtils.hasLength(forwardedPrefix)
							? forwardedPrefix
							: (StringUtils.hasLength(contextPath) ? contextPath : null);
					if (StringUtils.hasText(route.getPrefix())) {
						StringBuilder newPrefixBuilder = new StringBuilder();
						if (prefix != null) {
							if (prefix.endsWith("/")
									&& route.getPrefix().startsWith("/")) {
								newPrefixBuilder.append(prefix, 0,
										prefix.length() - 1);
							}
							else {
								newPrefixBuilder.append(prefix);
							}
						}
						newPrefixBuilder.append(route.getPrefix());
						prefix = newPrefixBuilder.toString();
					}
					if (prefix != null) {
						ctx.addZuulRequestHeader("X-Forwarded-Prefix", prefix);
					}
					String xforwardedfor = ctx.getRequest().getHeader("X-Forwarded-For");
					String remoteAddr = ctx.getRequest().getRemoteAddr();
					if (xforwardedfor == null) {
						xforwardedfor = remoteAddr;
					}
					else if (!xforwardedfor.contains(remoteAddr)) { // Prevent duplicates
						xforwardedfor += ", " + remoteAddr;
					}
					ctx.addZuulRequestHeader("X-Forwarded-For", xforwardedfor);
				}
				if (this.properties.isAddHostHeader()) {
					ctx.addZuulRequestHeader("Host", toHostHeader(ctx.getRequest()));
				}
			}
		}
		else {
			log.warn("No route found for uri: " + requestURI);

			String fallBackUri = requestURI;
			String fallbackPrefix = this.dispatcherServletPath; // default fallback
																// servlet is
																// DispatcherServlet

			if (RequestUtils.isZuulServletRequest()) {
				// remove the Zuul servletPath from the requestUri
				log.debug("zuulServletPath=" + this.properties.getServletPath());
				fallBackUri = fallBackUri.replaceFirst(this.properties.getServletPath(),
						"");
				log.debug("Replaced Zuul servlet path:" + fallBackUri);
			}
			else {
				// remove the DispatcherServlet servletPath from the requestUri
				log.debug("dispatcherServletPath=" + this.dispatcherServletPath);
				fallBackUri = fallBackUri.replaceFirst(this.dispatcherServletPath, "");
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

	private String toHostHeader(HttpServletRequest request) {
		int port = request.getServerPort();
		if ((port == 80 && "http".equals(request.getScheme())) || (port == 443 && "https".equals(request.getScheme()))) {
			return request.getServerName();
		} else {
			return request.getServerName() + ":" + port;
		}
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
