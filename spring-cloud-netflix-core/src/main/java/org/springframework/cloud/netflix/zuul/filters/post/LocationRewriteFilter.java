/*
 * Copyright 2017 the original author or authors.
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
 *
 */
package org.springframework.cloud.netflix.zuul.filters.post;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

import java.net.URI;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

/**
 * {@link ZuulFilter} Responsible for rewriting the Location header to be the Zuul URL
 *
 * @author Biju Kunjummen
 */
public class LocationRewriteFilter extends ZuulFilter {

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();

	@Autowired
	private ZuulProperties zuulProperties;

	@Autowired
	private RouteLocator routeLocator;

	private static final String LOCATION_HEADER = "Location";

	public LocationRewriteFilter() {
	}

	public LocationRewriteFilter(ZuulProperties zuulProperties,
			RouteLocator routeLocator) {
		this.routeLocator = routeLocator;
		this.zuulProperties = zuulProperties;
	}

	@Override
	public String filterType() {
		return POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return SEND_RESPONSE_FILTER_ORDER - 100;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		int statusCode = ctx.getResponseStatusCode();
		return HttpStatus.valueOf(statusCode).is3xxRedirection();
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		Route route = routeLocator.getMatchingRoute(
				urlPathHelper.getPathWithinApplication(ctx.getRequest()));

		if (route != null) {
			Pair<String, String> lh = locationHeader(ctx);
			if (lh != null) {
				String location = lh.second();
				URI originalRequestUri = UriComponentsBuilder
						.fromHttpRequest(new ServletServerHttpRequest(ctx.getRequest()))
						.build().toUri();

				UriComponentsBuilder redirectedUriBuilder = UriComponentsBuilder
						.fromUriString(location);

				UriComponents redirectedUriComps = redirectedUriBuilder.build();

				String newPath = getRestoredPath(this.zuulProperties, route,
						redirectedUriComps);

				String modifiedLocation = redirectedUriBuilder
						.scheme(originalRequestUri.getScheme())
						.host(originalRequestUri.getHost())
						.port(originalRequestUri.getPort()).replacePath(newPath).build()
						.toUriString();

				lh.setSecond(modifiedLocation);
			}
		}
		return null;
	}

	private String getRestoredPath(ZuulProperties zuulProperties, Route route,
			UriComponents redirectedUriComps) {
		StringBuilder path = new StringBuilder();
		String redirectedPathWithoutGlobal = downstreamHasGlobalPrefix(zuulProperties)
				? redirectedUriComps.getPath()
						.substring(("/" + zuulProperties.getPrefix()).length())
				: redirectedUriComps.getPath();

		if (downstreamHasGlobalPrefix(zuulProperties)) {
			path.append("/" + zuulProperties.getPrefix());
		}
		else {
			path.append(zuulHasGlobalPrefix(zuulProperties)
					? "/" + zuulProperties.getPrefix() : "");
		}

		path.append(downstreamHasRoutePrefix(route) ? "" : "/" + route.getPrefix())
				.append(redirectedPathWithoutGlobal);

		return path.toString();
	}

	private boolean downstreamHasGlobalPrefix(ZuulProperties zuulProperties) {
		return (!zuulProperties.isStripPrefix()
				&& StringUtils.hasText(zuulProperties.getPrefix()));
	}

	private boolean zuulHasGlobalPrefix(ZuulProperties zuulProperties) {
		return StringUtils.hasText(zuulProperties.getPrefix());
	}

	private boolean downstreamHasRoutePrefix(Route route) {
		return (!route.isPrefixStripped() && StringUtils.hasText(route.getPrefix()));
	}

	private Pair<String, String> locationHeader(RequestContext ctx) {
		if (ctx.getZuulResponseHeaders() != null) {
			for (Pair<String, String> pair : ctx.getZuulResponseHeaders()) {
				if (pair.first().equals(LOCATION_HEADER)) {
					return pair;
				}
			}
		}
		return null;
	}
}
