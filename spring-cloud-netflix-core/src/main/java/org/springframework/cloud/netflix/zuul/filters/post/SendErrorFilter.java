package org.springframework.cloud.netflix.zuul.filters.post;

import com.google.common.base.Throwables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.RequestDispatcher;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class SendErrorFilter extends ZuulFilter {

    protected static final String SEND_ERROR_FILTER_RAN = "sendErrorFilter.ran";

    @Value("${error.path:/error}")
    private String errorPath;

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        //only forward to errorPath if it hasn't been forwarded to already
        return ctx.containsKey("error.status_code") && !ctx.getBoolean(SEND_ERROR_FILTER_RAN, false);
    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            int statusCode = (Integer)ctx.get("error.status_code");
            if (ctx.containsKey("error.exception")) {
                Object e = ctx.get("error.exception");
                log.warn("Error during filtering", Throwable.class.cast(e));
                ctx.getRequest().setAttribute("javax.servlet.error.exception", e);
            }
            ctx.getRequest().setAttribute("javax.servlet.error.status_code", statusCode);
			RequestDispatcher dispatcher = ctx.getRequest().getRequestDispatcher(errorPath);
			if (dispatcher != null) {
				ctx.set(SEND_ERROR_FILTER_RAN, true);
                if (!ctx.getResponse().isCommitted()) {
                    dispatcher.forward(ctx.getRequest(), ctx.getResponse());
                }
			}
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
}
