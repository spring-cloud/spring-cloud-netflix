package org.springframework.cloud.netflix.zuul.util;

import com.netflix.zuul.context.RequestContext;

public class RequestUtils {
    
    public static final String IS_DISPATCHERSERVLETREQUEST = "isDispatcherServletRequest";
    
    public static boolean isDispatcherServletRequest() {
        return RequestContext.getCurrentContext().getBoolean(IS_DISPATCHERSERVLETREQUEST);
    }
    
    public static boolean isZuulServletRequest() {
        //extra check for dispatcher since ZuulServlet can run from ZuulController
        return !isDispatcherServletRequest() && RequestContext.getCurrentContext().getZuulEngineRan();
    }    
}
