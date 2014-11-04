package org.springframework.cloud.netflix.zuul.filters.pre;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.Routes;
import org.springframework.cloud.netflix.zuul.ZuulProperties;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;

public class PreDecorationFilter extends ZuulFilter {
    private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

    @Autowired
    private Routes routes;

    @Autowired
    private ZuulProperties properties;

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

        LinkedHashMap<String, String> routesMap = routes.getRoutes();

        Optional<String> route = Iterables.tryFind(routesMap.keySet(), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String path) {
                return uriPart.startsWith(path);
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
            //TODO: 404
        }
        return null;
    }
}
