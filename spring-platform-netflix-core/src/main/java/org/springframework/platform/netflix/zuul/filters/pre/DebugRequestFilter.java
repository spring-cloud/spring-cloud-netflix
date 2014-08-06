package org.springframework.platform.netflix.zuul.filters.pre;

import com.netflix.zuul.ZuulFilter;

import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class DebugRequestFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10000;
    }

    @Override
    public boolean shouldFilter() {
        return Debug.debugRequest();
    }

    @Override
    public Object run() {
        HttpServletRequest req = RequestContext.getCurrentContext().getRequest();

        Debug.addRequestDebug("REQUEST:: " + req.getScheme() + " " + req.getRemoteAddr() + ":" + req.getRemotePort());

        Debug.addRequestDebug("REQUEST:: > " + req.getMethod() + " " + req.getRequestURI() + " " + req.getProtocol());

        Enumeration<String> headerIt = req.getHeaderNames();
        while (headerIt.hasMoreElements()) {
            String name = headerIt.nextElement();
            String value = req.getHeader(name);
            Debug.addRequestDebug("REQUEST:: > " + name + ":" + value);

        }

        final RequestContext ctx = RequestContext.getCurrentContext();
        if (!ctx.isChunkedRequestBody()) {
            try {
                InputStream inp = ctx.getRequest().getInputStream();
                if (inp != null) {
                    String body = IOUtils.toString(inp);
                    Debug.addRequestDebug("REQUEST:: > " + body);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

}
