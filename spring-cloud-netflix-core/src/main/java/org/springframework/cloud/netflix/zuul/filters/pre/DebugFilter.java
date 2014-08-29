package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

import javax.servlet.http.HttpServletRequest;

public class DebugFilter extends ZuulFilter {

    static final DynamicBooleanProperty routingDebug = DynamicPropertyFactory.getInstance()
            .getBooleanProperty(ZuulConstants.ZUUL_DEBUG_REQUEST, false);
    static final DynamicStringProperty debugParameter = DynamicPropertyFactory.getInstance()
            .getStringProperty(ZuulConstants.ZUUL_DEBUG_PARAMETER, "debug");

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    public boolean shouldFilter() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        if ("true".equals(request.getParameter(debugParameter.get())))
            return true;

        return routingDebug.get();
    }

    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setDebugRouting(true);
        ctx.setDebugRequest(true);
        return null;
    }


}



