package org.springframework.cloud.netflix.endpoint;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: move to spring-boot?
 * User: spencergibb
 * Date: 4/24/14
 * Time: 9:13 PM
 */
public abstract class ServletWrappingEndpoint implements InitializingBean,
        ApplicationContextAware, ServletContextAware, MvcEndpoint {

    protected String path;
    protected boolean sensitive;
    protected boolean enabled = true;

    protected final ServletWrappingController controller = new ServletWrappingController();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.controller.afterPropertiesSet();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.controller.setServletContext(servletContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.controller.setApplicationContext(applicationContext);
    }

    protected ServletWrappingEndpoint(Class<?> servletClass, String servletName, String path,
                                      boolean sensitive, boolean enabled) {
        controller.setServletClass(servletClass);
        controller.setServletName(servletName);
        this.path = path;
        this.sensitive = sensitive;
        this.enabled = enabled;
    }

    @RequestMapping("**")
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return this.controller.handleRequest(request, response);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isSensitive() {
        return sensitive;
    }

    @Override
    public Class<? extends Endpoint<?>> getEndpointType() {
        return null;
    }
}
