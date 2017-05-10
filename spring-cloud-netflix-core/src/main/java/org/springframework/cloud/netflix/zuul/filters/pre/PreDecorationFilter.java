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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_LOCATION_PREFIX;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTPS_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTPS_SCHEME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTP_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTP_SCHEME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RETRYABLE_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_FORWARDED_FOR_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_FORWARDED_HOST_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_FORWARDED_PORT_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_FORWARDED_PREFIX_HEADER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_FORWARDED_PROTO_HEADER;

/**
 * Pre {@link ZuulFilter} that determines where and how to route based on the supplied {@link RouteLocator}.
 * Also sets various proxy related headers for downstream requests.
 */
public class PreDecorationFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(PreDecorationFilter.class);

	/**
	 * @deprecated use {@link FilterConstants#PRE_DECORATION_FILTER_ORDER}
	 */
	@Deprecated
	public static final int FILTER_ORDER = PRE_DECORATION_FILTER_ORDER;

	private RouteLocator routeLocator;

	private String dispatcherServletPath;

	private ZuulProperties properties;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private ProxyRequestHelper proxyRequestHelper;

	public PreDecorationFilter(RouteLocator routeLocator, String dispatcherServletPath, ZuulProperties properties,
			ProxyRequestHelper proxyRequestHelper) {
		this.routeLocator = routeLocator;
		this.properties = properties;
		this.urlPathHelper.setRemoveSemicolonContent(properties.isRemoveSemicolonContent());
		this.dispatcherServletPath = dispatcherServletPath;
		this.proxyRequestHelper = proxyRequestHelper;
	}

	@Override
	public int filterOrder() {
		return PRE_DECORATION_FILTER_ORDER;
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return !ctx.containsKey(FORWARD_TO_KEY) // a filter has already forwarded
				&& !ctx.containsKey(SERVICE_ID_KEY); // a filter has already determined serviceId
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx.getRequest());
		Route route = this.routeLocator.getMatchingRoute(requestURI);
		if (route != null) {
			String location = route.getLocation();
			if (location != null) {
				ctx.put(REQUEST_URI_KEY, route.getPath());
				ctx.put(PROXY_KEY, route.getId());
				if (!route.isCustomSensitiveHeaders()) {
					this.proxyRequestHelper
							.addIgnoredHeaders(this.properties.getSensitiveHeaders().toArray(new String[0]));
				}
				else {
					this.proxyRequestHelper.addIgnoredHeaders(route.getSensitiveHeaders().toArray(new String[0]));
				}

				if (route.getRetryable() != null) {
					ctx.put(RETRYABLE_KEY, route.getRetryable());
				}

				if (location.startsWith(HTTP_SCHEME+":") || location.startsWith(HTTPS_SCHEME+":")) {
					ctx.setRouteHost(getUrl(location));
					ctx.addOriginResponseHeader(SERVICE_HEADER, location);
				}
				else if (location.startsWith(FORWARD_LOCATION_PREFIX)) {
					ctx.set(FORWARD_TO_KEY,
							StringUtils.cleanPath(location.substring(FORWARD_LOCATION_PREFIX.length()) + route.getPath()));
					ctx.setRouteHost(null);
					return null;
				}
				else {
					// set serviceId for use in filters.route.RibbonRequest
					ctx.set(SERVICE_ID_KEY, location);
					ctx.setRouteHost(null);
					ctx.addOriginResponseHeader(SERVICE_ID_HEADER, location);
				}
				if (this.properties.isAddProxyHeaders()) {
					addProxyHeaders(ctx, route);
					String xforwardedfor = ctx.getRequest().getHeader(X_FORWARDED_FOR_HEADER);
					String remoteAddr = ctx.getRequest().getRemoteAddr();
					if (xforwardedfor == null) {
						xforwardedfor = remoteAddr;
					}
					else if (!xforwardedfor.contains(remoteAddr)) { // Prevent duplicates
						xforwardedfor += ", " + remoteAddr;
					}
					ctx.addZuulRequestHeader(X_FORWARDED_FOR_HEADER, xforwardedfor);
				}
				if (this.properties.isAddHostHeader()) {
					ctx.addZuulRequestHeader(HttpHeaders.HOST, toHostHeader(ctx.getRequest()));
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
				fallBackUri = fallBackUri.replaceFirst(this.properties.getServletPath(), "");
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
			ctx.set(FORWARD_TO_KEY, forwardURI);
		}
		return null;
	}

	private void addProxyHeaders(RequestContext ctx, Route route) {
		HttpServletRequest request = ctx.getRequest();
		String host = toHostHeader(request);
		String port = String.valueOf(request.getServerPort());
		String proto = request.getScheme();
		if (hasHeader(request, X_FORWARDED_HOST_HEADER)) {
			host = request.getHeader(X_FORWARDED_HOST_HEADER) + "," + host;
		}
		if (!hasHeader(request, X_FORWARDED_PORT_HEADER)) {
			if (hasHeader(request, X_FORWARDED_PROTO_HEADER)) {
				StringBuilder builder = new StringBuilder();
				for (String previous : StringUtils.commaDelimitedListToStringArray(request.getHeader(X_FORWARDED_PROTO_HEADER))) {
					if (builder.length()>0) {
						builder.append(",");
					}
					builder.append(HTTPS_SCHEME.equals(previous) ? HTTPS_PORT : HTTP_PORT);
				}
				builder.append(",").append(port);
				port = builder.toString();
			}
		} else {
			port = request.getHeader(X_FORWARDED_PORT_HEADER) + "," + port;
		}
		if (hasHeader(request, X_FORWARDED_PROTO_HEADER)) {
			proto = request.getHeader(X_FORWARDED_PROTO_HEADER) + "," + proto;
		}
		ctx.addZuulRequestHeader(X_FORWARDED_HOST_HEADER, host);
		ctx.addZuulRequestHeader(X_FORWARDED_PORT_HEADER, port);
		ctx.addZuulRequestHeader(X_FORWARDED_PROTO_HEADER, proto);
		addProxyPrefix(ctx, route);
	}

	private boolean hasHeader(HttpServletRequest request, String name) {
		return StringUtils.hasLength(request.getHeader(name));
	}

	private void addProxyPrefix(RequestContext ctx, Route route) {
		String forwardedPrefix = ctx.getRequest().getHeader(X_FORWARDED_PREFIX_HEADER);
		String contextPath = ctx.getRequest().getContextPath();
		String prefix = StringUtils.hasLength(forwardedPrefix) ? forwardedPrefix
				: (StringUtils.hasLength(contextPath) ? contextPath : null);
		if (StringUtils.hasText(route.getPrefix())) {
			StringBuilder newPrefixBuilder = new StringBuilder();
			if (prefix != null) {
				if (prefix.endsWith("/") && route.getPrefix().startsWith("/")) {
					newPrefixBuilder.append(prefix, 0, prefix.length() - 1);
				}
				else {
					newPrefixBuilder.append(prefix);
				}
			}
			newPrefixBuilder.append(route.getPrefix());
			prefix = newPrefixBuilder.toString();
		}
		if (prefix != null) {
			ctx.addZuulRequestHeader(X_FORWARDED_PREFIX_HEADER, prefix);
		}
	}

	private String toHostHeader(HttpServletRequest request) {
		int port = request.getServerPort();
		if ((port == HTTP_PORT && HTTP_SCHEME.equals(request.getScheme()))
				|| (port == HTTPS_PORT && HTTPS_SCHEME.equals(request.getScheme()))) {
			return request.getServerName();
		}
		else {
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
