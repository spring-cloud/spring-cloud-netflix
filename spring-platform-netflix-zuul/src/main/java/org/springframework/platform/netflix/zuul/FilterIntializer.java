package org.springframework.platform.netflix.zuul;

import com.netflix.zuul.FilterFileManager;
import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.groovy.GroovyFileFilter;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
//import javax.servlet.http.HttpSessionEvent;

/**
 * User: spencergibb
 * Date: 4/24/14
 * Time: 9:23 PM
 * TODO:  .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
 */
public class FilterIntializer implements ServletContextListener/*, HttpSessionListener*/ {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterIntializer.class);

    @Autowired
    ZuulProperties props;

    @Override
    /*public void sessionCreated(HttpSessionEvent se) {
        contextInitialized(null);
    }*/
    public void contextInitialized(ServletContextEvent sce) {

        LOGGER.info("Starting filter initialzer context listener");

        //FIXME: mocks monitoring infrastructure as we don't need it for this simple app
        MonitoringHelper.initMocks();

        // initializes groovy filesystem poller
        initGroovyFilterManager();
    }

    @Override
    /*public void sessionDestroyed(HttpSessionEvent se) {
        contextDestroyed(null);
    }*/
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Stopping filter initializer context listener");
    }

    private void initGroovyFilterManager() {

        //TODO: pluggable filter initialzer
        FilterLoader.getInstance().setCompiler(new GroovyCompiler());

        final String scriptRoot = props.getFilterRoot();
        LOGGER.info("Using file system script: " + scriptRoot);

        try {
            FilterFileManager.setFilenameFilter(new GroovyFileFilter());
            FilterFileManager.init(5,
                    scriptRoot + "/pre",
                    scriptRoot + "/route",
                    scriptRoot + "/post"
            );
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
