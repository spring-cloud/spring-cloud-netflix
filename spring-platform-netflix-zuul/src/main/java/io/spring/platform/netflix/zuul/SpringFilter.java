package io.spring.platform.netflix.zuul;

import com.netflix.zuul.ZuulFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * User: spencergibb
 * Date: 5/1/14
 * Time: 10:59 PM
 */
public abstract class SpringFilter extends ZuulFilter {

    protected <T> T getBean(Class<T> beanClass) {
        //FIXME: hack because zuul uses servlet 2.5?
        RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
        if (!(requestAttr instanceof ServletRequestAttributes)) {
            throw new IllegalStateException("Current request is not a servlet request");
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttr;
        HttpServletRequest request = attributes.getRequest();

        WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
        return context.getBean(beanClass);
    }
}
