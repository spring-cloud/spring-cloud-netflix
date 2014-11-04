package org.springframework.cloud.netflix.zuul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.PropertySourceUtils;
import org.springframework.core.env.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Spencer Gibb
 */
public class Routes {
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static final String DEFAULT_ROUTE = "/";

    @Autowired
    ConfigurableEnvironment env;
    private Field propertySourcesField;
    private String keyPrefix;

    public Routes(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        initField();
    }

    private void initField() {
        propertySourcesField = ReflectionUtils.findField(CompositePropertySource.class, "propertySources");
        propertySourcesField.setAccessible(true);
    }

    //TODO: cache routes or respond to environment event and refresh all routes
    public LinkedHashMap<String, String> getRoutes() {
        LinkedHashMap<String, String> routes = new LinkedHashMap<>();
        MutablePropertySources propertySources = env.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            getRoutes(propertySource, routes);
        }

        String defaultServiceId = routes.get(DEFAULT_ROUTE);

        if (defaultServiceId != null) {
            //move the defaultServiceId to the end
            routes.remove(DEFAULT_ROUTE);
            routes.put(DEFAULT_ROUTE, defaultServiceId);
        }

        return routes;
    }

    public void getRoutes(PropertySource<?> propertySource, LinkedHashMap<String, String> routes) {
        if (propertySource instanceof CompositePropertySource) {
            try {
                @SuppressWarnings("unchecked")
                Set<PropertySource<?>> sources = (Set<PropertySource<?>>) propertySourcesField.get(propertySource);
                for (PropertySource<?> source : sources) {
                    getRoutes(source, routes);
                }
            } catch (IllegalAccessException e) {
                return;
            }
        } else {
            //EnumerablePropertySource enumerable = (EnumerablePropertySource) propertySource;
            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addLast(propertySource);
            Map<String, Object> routeEntries = PropertySourceUtils.getSubProperties(propertySources, keyPrefix);
            for (Map.Entry<String, Object> entry : routeEntries.entrySet()) {
                String serviceId = entry.getKey();
                String route = entry.getValue().toString();

                if (routes.containsKey(route)) {
                    logger.warn("Overwriting route {}: already defined by {}", route, routes.get(route));
                }
                routes.put(route, serviceId);
            }
        }
    }
}
