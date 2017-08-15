package org.springframework.cloud.netflix.zuul.filters.pre;

import com.google.common.io.ByteStreams;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Filter which prepares the request's input stream to be retryable
 *
 * @author Andre Dörnbrack
 */
public class ResettableInputStreamWrapperFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return RESETTABLE_INPUT_STREAM_WRAPPER_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        return hasContentLength(request);
    }

    private boolean hasContentLength(HttpServletRequest request) {
        return request.getContentLength() >= 0
            || hasFormBodyContentType(request);
    }

    private boolean hasFormBodyContentType(HttpServletRequest request) {
        try {
            MediaType mediaType = MediaType.valueOf(request.getContentType());
            return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
                    || (isDispatcherServletRequest(request)
                    && MediaType.MULTIPART_FORM_DATA.includes(mediaType));
        }
        catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    private boolean isDispatcherServletRequest(HttpServletRequest request) {
        return request.getAttribute(
                DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        HttpServletRequestWrapper httpServletRequestWrapper = new HttpServletRequestWrapper(request) {
            private ResettableServletInputStreamWrapper ResettableServletInputStreamWrapper;

            @Override
            public ServletInputStream getInputStream() throws IOException {
                if (ResettableServletInputStreamWrapper == null) {
                    ResettableServletInputStreamWrapper = new ResettableServletInputStreamWrapper(ByteStreams.toByteArray(super.getInputStream()));
                }

                return ResettableServletInputStreamWrapper;
            }
        };

        try {
            ctx.setRequest(httpServletRequestWrapper);
            ctx.set(REQUEST_ENTITY_KEY, httpServletRequestWrapper.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set request entity in request context", e);
        }

        return true;
    }
}
