package io.spring.platform.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import javax.servlet.ServletContext;

/**
 * User: spencergibb
 * Date: 5/1/14
 * Time: 10:03 AM
 */
public abstract class AbstractServletWrappingController implements InitializingBean,
        ApplicationContextAware, ServletContextAware {

    protected final ServletWrappingController controller = new ServletWrappingController();

    public AbstractServletWrappingController(Class<?> servletClass, String servletName) {
        controller.setServletClass(servletClass);
        controller.setServletName(servletName);
    }

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

}
