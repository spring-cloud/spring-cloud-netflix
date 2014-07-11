package filters.pre

import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.DynamicStringProperty
import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.constants.ZuulConstants
import com.netflix.zuul.context.RequestContext

class DebugFilter extends ZuulFilter {

    static final DynamicBooleanProperty routingDebug = DynamicPropertyFactory.getInstance().getBooleanProperty(ZuulConstants.ZUUL_DEBUG_REQUEST, true)
    static final DynamicStringProperty debugParameter = DynamicPropertyFactory.getInstance().getStringProperty(ZuulConstants.ZUUL_DEBUG_PARAMETER, "d")

    @Override
    String filterType() {
        return 'pre'
    }

    @Override
    int filterOrder() {
        return 1
    }

    boolean shouldFilter() {
        if ("true".equals(RequestContext.getCurrentContext().getRequest().getParameter(debugParameter.get()))) return true;
        return routingDebug.get();

    }

    Object run() {
        RequestContext ctx = RequestContext.getCurrentContext()
        ctx.setDebugRouting(true)
        ctx.setDebugRequest(true)
        return null;
    }


}



