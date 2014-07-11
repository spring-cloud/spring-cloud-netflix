package filters.pre

import com.netflix.zuul.ZuulFilter

import com.netflix.zuul.context.Debug
import com.netflix.zuul.context.RequestContext

import javax.servlet.http.HttpServletRequest

class DebugRequest extends ZuulFilter {
    @Override
    String filterType() {
        return 'pre'
    }

    @Override
    int filterOrder() {
        return 10000
    }

    @Override
    boolean shouldFilter() {
        return Debug.debugRequest()
    }

    @Override
    Object run() {
        HttpServletRequest req = RequestContext.currentContext.request as HttpServletRequest

        Debug.addRequestDebug("REQUEST:: " + req.getScheme() + " " + req.getRemoteAddr() + ":" + req.getRemotePort())

        Debug.addRequestDebug("REQUEST:: > " + req.getMethod() + " " + req.getRequestURI() + " " + req.getProtocol())

        Iterator headerIt = req.getHeaderNames().iterator()
        while (headerIt.hasNext()) {
            String name = (String) headerIt.next()
            String value = req.getHeader(name)
            Debug.addRequestDebug("REQUEST:: > " + name + ":" + value)

        }

        final RequestContext ctx = RequestContext.getCurrentContext()
        if (!ctx.isChunkedRequestBody()) {
            InputStream inp = ctx.request.getInputStream()
            String body = null
            if (inp != null) {
                body = inp.getText()
                Debug.addRequestDebug("REQUEST:: > " + body)

            }
        }
        return null;
    }

}
