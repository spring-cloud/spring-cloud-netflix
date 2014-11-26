package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ZuulServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Spencer Gibb
 */
public class ZuulController extends ServletWrappingController {

    public ZuulController() {
        setServletClass(ZuulServlet.class);
        setServletName("zuul");
        setSupportedMethods((String[])null); // Allow all
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            return super.handleRequestInternal(request, response);
        } finally {
            // @see com.netflix.zuul.context.ContextLifecycleFilter.doFilter
            RequestContext.getCurrentContext().unset();
        }
    }
}
