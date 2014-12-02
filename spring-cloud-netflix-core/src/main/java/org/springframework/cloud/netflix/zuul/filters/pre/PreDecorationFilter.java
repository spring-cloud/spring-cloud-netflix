package org.springframework.cloud.netflix.zuul.filters.pre;

import static com.google.common.collect.Iterables.tryFind;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.ZuulRouteLocator;
import org.springframework.cloud.netflix.zuul.ZuulProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PreDecorationFilter extends ZuulFilter {
	private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

	private ZuulRouteLocator routeLocator;

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	public PreDecorationFilter(ZuulRouteLocator routeLocator, ZuulProperties properties) {
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

		String requestURI = ctx.getRequest().getRequestURI();

		String proxyMapping = properties.getPrefix();

		final String uriPart;
		if (StringUtils.hasText(proxyMapping) && properties.isStripPrefix()
				&& requestURI.startsWith(proxyMapping)) {
			// TODO: better strategy?
			uriPart = requestURI.substring(proxyMapping.length());
		}
		else {
			uriPart = requestURI;
		}
		ctx.put("requestURI", uriPart);

		Map<String, String> routesMap = routeLocator.getRoutes();

		Optional<String> route = tryFind(routesMap.keySet(), new Predicate<String>() {
			@Override
			public boolean apply(@Nullable String path) {
				return pathMatcher.match(path, uriPart);
			}
		});

		if (route.isPresent()) {
			String target = routesMap.get(route.get());

			if (target != null) {

				if (target.startsWith("http:") || target.startsWith("https:")) {
					ctx.setRouteHost(getUrl(target));
					ctx.addOriginResponseHeader("X-Zuul-Service", target);
				}
				else {
					// set serviceId for use in filters.route.RibbonRequest
					ctx.set("serviceId", target);
					ctx.setRouteHost(null);
					ctx.addOriginResponseHeader("X-Zuul-ServiceId", target);
				}

				if (properties.isAddProxyHeaders()) {
					ctx.addZuulRequestHeader(
							"X-Forwarded-Host",
							ctx.getRequest().getServerName() + ":"
									+ String.valueOf(ctx.getRequest().getServerPort()));
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
