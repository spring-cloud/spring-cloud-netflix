package org.springframework.platform.netflix.zuul.filters.pre;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.platform.netflix.zuul.Routes;
import org.springframework.platform.netflix.zuul.ZuulProxyProperties;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;

public class PreDecorationFilter extends ZuulFilter {
    private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

    @Autowired
    private Routes routes;

    @Autowired
    private ZuulProxyProperties properties;

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

        //remove proxy prefix TODO: only if embedded proxy
        String proxyMapping = properties.getMapping();
        final String uriPart = requestURI.replace(proxyMapping, ""); //TODO: better strategy?
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
            }
        } else {
            LOG.warn("No route found for uri: "+requestURI);
            //TODO: 404
        }
        return null;
    }
}
