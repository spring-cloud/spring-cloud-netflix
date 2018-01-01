package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.web.util.UrlPathHelper;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTP_METHOD_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;


public class HttpMethodFilter extends ZuulFilter {

    private static final Log log = LogFactory.getLog(HttpMethodFilter.class);

    private RouteLocator routeLocator;

    private ProxyRequestHelper proxyRequestHelper;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public HttpMethodFilter(RouteLocator routeLocator, ProxyRequestHelper proxyRequestHelper) {
        this.routeLocator = routeLocator;
        this.proxyRequestHelper = proxyRequestHelper;
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return HTTP_METHOD_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx.getRequest());
        final String requestMethod = ctx.getRequest().getMethod();
        Route route = this.routeLocator.getMatchingRoute(requestURI, requestMethod);
        if (route != null) {
            this.proxyRequestHelper.setAllowedMethods(route.getMethods());
        } else {
            log.warn("No route found for uri: " + requestURI);
        }
        return null;
    }
}
