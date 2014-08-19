package org.springframework.platform.netflix.zuul.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import java.util.List;

public class StatsFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 2000;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        dumpRoutingDebug();
        dumpRequestDebug();
        return null;
    }

    public void dumpRequestDebug() {
        @SuppressWarnings("unchecked")
		List<String> rd = (List<String>) RequestContext.getCurrentContext().get("requestDebug");
        if (rd != null) {
            for (String it : rd) {
                System.out.println("REQUEST_DEBUG::" + it);
            }
        }
    }

    public void dumpRoutingDebug() {
        @SuppressWarnings("unchecked")
		List<String> rd = (List<String>) RequestContext.getCurrentContext().get("routingDebug");
        if (rd != null) {
            for (String it : rd) {
                System.out.println("ZUUL_DEBUG::"+it);
            }
        }
    }

}
