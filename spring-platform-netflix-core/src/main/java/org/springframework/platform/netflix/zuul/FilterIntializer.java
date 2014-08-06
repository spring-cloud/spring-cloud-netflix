package org.springframework.platform.netflix.zuul;

import com.netflix.zuul.FilterFileManager;
import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.groovy.GroovyFileFilter;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;
import java.util.Map;

/**
 * User: spencergibb
 * Date: 4/24/14
 * Time: 9:23 PM
 * TODO:  .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
 */
public class FilterIntializer implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterIntializer.class);

    @Autowired
    private Map<String, ZuulFilter> filters;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        LOGGER.info("Starting filter initialzer context listener");

        //FIXME: mocks monitoring infrastructure as we don't need it for this simple app
        MonitoringHelper.initMocks();

        FilterRegistry registry = FilterRegistry.instance();

        for (Map.Entry<String, ZuulFilter> entry : filters.entrySet()) {
            registry.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Stopping filter initializer context listener");
    }

    /*private void initGroovyFilterManager() {

        //TODO: support groovy filters loaded from filesystem in proxy
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
    }*/
}
