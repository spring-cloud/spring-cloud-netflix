package org.springframework.cloud.netflix.zuul.filters.pre;

import com.google.common.base.Throwables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * @author Spencer Gibb
 */
public class Servlet30WrapperFilter extends ZuulFilter {
    protected Field requestField = null;

    public Servlet30WrapperFilter() {
        requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "req",
                HttpServletRequest.class);
        Assert.notNull(requestField, "HttpServletRequestWrapper.req field not found");
        requestField.setAccessible(true);
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true; //TODO: only if in servlet 3.0 env
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        if (request instanceof HttpServletRequestWrapper) {
            try {
                request = (HttpServletRequest) requestField.get(request);
            } catch (IllegalAccessException e) {
                Throwables.propagate(e);
            }
        }
        ctx.setRequest(new Servlet30RequestWrapper(request));
        //ctx.setResponse(new HttpServletResponseWrapper(ctx.getResponse()));
        return null;
    }

    private class Servlet30RequestWrapper extends HttpServletRequestWrapper {
        private HttpServletRequest request;

        Servlet30RequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }

        @Override
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            return request.authenticate(response);
        }

        @Override
        public void login(String username, String password) throws ServletException {
            request.login(username, password);
        }

        @Override
        public void logout() throws ServletException {
            request.logout();
        }

        @Override
        public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
            return request.getParts();
        }

        @Override
        public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
            return request.getPart(name);
        }

        @Override
        public ServletContext getServletContext() {
            return request.getServletContext();
        }

        @Override
        public AsyncContext startAsync() {
            return request.startAsync();
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
            return request.startAsync(servletRequest, servletResponse);
        }

        @Override
        public boolean isAsyncStarted() {
            return request.isAsyncStarted();
        }

        @Override
        public boolean isAsyncSupported() {
            return request.isAsyncSupported();
        }

        @Override
        public AsyncContext getAsyncContext() {
            return request.getAsyncContext();
        }

        @Override
        public DispatcherType getDispatcherType() {
            return request.getDispatcherType();
        }
    }
}
