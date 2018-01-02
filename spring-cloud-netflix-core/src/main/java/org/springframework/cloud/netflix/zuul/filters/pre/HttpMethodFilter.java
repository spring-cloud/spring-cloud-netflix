package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.web.util.UrlPathHelper;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTP_METHOD_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;


public class HttpMethodFilter extends ZuulFilter {

    private static final Log log = LogFactory.getLog(HttpMethodFilter.class);

    private RouteLocator routeLocator;

    private ProxyRequestHelper proxyRequestHelper;

    private String dispatcherServletPath;

    private ZuulProperties properties;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public HttpMethodFilter(RouteLocator routeLocator, ProxyRequestHelper proxyRequestHelper,
                            String dispatcherServletPath, ZuulProperties properties) {
        this.routeLocator = routeLocator;
        this.proxyRequestHelper = proxyRequestHelper;
        this.dispatcherServletPath = dispatcherServletPath;
        this.properties = properties;
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
}
