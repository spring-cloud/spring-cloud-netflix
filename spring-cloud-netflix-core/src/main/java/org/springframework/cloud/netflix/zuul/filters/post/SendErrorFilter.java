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
        return ctx.containsKey("error.status_code");
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
                dispatcher.forward(ctx.getRequest(), ctx.getResponse());
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
