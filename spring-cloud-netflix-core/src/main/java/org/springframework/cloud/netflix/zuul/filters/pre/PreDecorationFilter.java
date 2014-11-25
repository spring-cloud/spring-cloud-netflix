package org.springframework.cloud.netflix.zuul.filters.pre;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.RouteLocator;
import org.springframework.cloud.netflix.zuul.ZuulProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.google.common.collect.Iterables.*;

public class PreDecorationFilter extends ZuulFilter {
    private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ZuulProperties properties;

    private PathMatcher pathMatcher = new AntPathMatcher();

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

        String proxyMapping = properties.getMapping();

        final String uriPart;
        if (properties.isStripMapping()) {
            uriPart = requestURI.replace(proxyMapping, ""); //TODO: better strategy?
        } else {
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
            String serviceId = routesMap.get(route.get());

            if (serviceId != null) {
                // set serviceId for use in filters.route.RibbonRequest
                ctx.set("serviceId", serviceId);
                ctx.setRouteHost(null);
                ctx.addOriginResponseHeader("X-Zuul-ServiceId", serviceId);

                if (properties.isAddProxyHeaders()) {
                    ctx.addZuulRequestHeader("X-Forwarded-Host", ctx.getRequest().getServerName() + ":" + String.valueOf(ctx.getRequest().getServerPort()));
                }
            }
        } else {
            LOG.warn("No route found for uri: "+requestURI);
            ctx.set("error.status_code", HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }
}
