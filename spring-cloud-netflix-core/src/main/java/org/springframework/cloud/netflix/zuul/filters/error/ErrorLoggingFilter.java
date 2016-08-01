package org.springframework.cloud.netflix.zuul.filters.error;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ErrorLoggingFilter extends ZuulFilter {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getThrowable() != null;
    }

    @Override
    public Object run() {
        Throwable throwable = RequestContext.getCurrentContext().getThrowable();
        logger.error("Error during processing request:", throwable);
        return null;
    }
}