package filters.post

import com.netflix.zuul.ZuulFilter

import com.netflix.zuul.context.RequestContext

class Stats extends ZuulFilter {
    @Override
    String filterType() {
        return "post"
    }

    @Override
    int filterOrder() {
        return 2000
    }

    @Override
    boolean shouldFilter() {
        return true
    }

    @Override
    Object run() {
        dumpRoutingDebug()
        dumpRequestDebug()
    }

    public void dumpRequestDebug() {
        List<String> rd = (List<String>) RequestContext.getCurrentContext().get("requestDebug");
        rd?.each {
            println("REQUEST_DEBUG::${it}");
        }
    }

    public void dumpRoutingDebug() {
        List<String> rd = (List<String>) RequestContext.getCurrentContext().get("routingDebug");
        rd?.each {
            println("ZUUL_DEBUG::${it}");
        }
    }

}
