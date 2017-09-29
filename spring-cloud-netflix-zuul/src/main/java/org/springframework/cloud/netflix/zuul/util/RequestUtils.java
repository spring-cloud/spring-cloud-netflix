package org.springframework.cloud.netflix.zuul.util;

import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.IS_DISPATCHER_SERVLET_REQUEST_KEY;

public class RequestUtils {

    /**
     * @deprecated use {@link org.springframework.cloud.netflix.zuul.filters.support.FilterConstants#IS_DISPATCHER_SERVLET_REQUEST_KEY}
     */
    @Deprecated
    public static final String IS_DISPATCHERSERVLETREQUEST = IS_DISPATCHER_SERVLET_REQUEST_KEY;
    
    public static boolean isDispatcherServletRequest() {
        return RequestContext.getCurrentContext().getBoolean(IS_DISPATCHER_SERVLET_REQUEST_KEY);
    }
    
    public static boolean isZuulServletRequest() {
        //extra check for dispatcher since ZuulServlet can run from ZuulController
        return !isDispatcherServletRequest() && RequestContext.getCurrentContext().getZuulEngineRan();
    }    
}
