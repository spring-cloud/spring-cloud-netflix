package org.springframework.cloud.netflix.zuul;

import java.lang.reflect.Field;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.monitoring.MonitoringHelper;

/**
 * @author Spencer Gibb
 * 
 * TODO:  .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
 */
public class ZuulFilterInitializer implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulFilterInitializer.class);

    private Map<String, ZuulFilter> filters;

	public ZuulFilterInitializer(Map<String, ZuulFilter> filters) {
		this.filters = filters;
	}

	@Override
    public void contextInitialized(ServletContextEvent sce) {

        LOGGER.info("Starting filter initializer context listener");

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
        FilterRegistry registry = FilterRegistry.instance();
        for (Map.Entry<String, ZuulFilter> entry : filters.entrySet()) {
            registry.remove(entry.getKey());
        }
        clearLoaderCache();
    }

	private void clearLoaderCache() {
		FilterLoader instance = FilterLoader.getInstance();
		Field field = ReflectionUtils.findField(FilterLoader.class, "hashFiltersByType");
		ReflectionUtils.makeAccessible(field);
		@SuppressWarnings("rawtypes")
		Map cache = (Map) ReflectionUtils.getField(field, instance);
		cache.clear();
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
