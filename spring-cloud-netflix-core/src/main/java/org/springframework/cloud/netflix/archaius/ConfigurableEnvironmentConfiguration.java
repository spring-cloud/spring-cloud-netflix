package org.springframework.cloud.netflix.archaius;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Spencer Gibb
 */
public class ConfigurableEnvironmentConfiguration extends AbstractConfiguration {
    ConfigurableEnvironment environment;

    public ConfigurableEnvironmentConfiguration(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {

    }

    @Override
    public boolean isEmpty() {
        return !getKeys().hasNext(); //TODO: find a better way to do this
    }

    @Override
    public boolean containsKey(String key) {
        return environment.containsProperty(key);
    }

    @Override
    public Object getProperty(String key) {
        return environment.getProperty(key);
    }

    @Override
    public Iterator<String> getKeys() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PropertySource<?>> entry : getPropertySources().entrySet()) {
            PropertySource<?> source = entry.getValue();
            if (source instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
                for (String name : enumerable.getPropertyNames()) {
                    result.add(name);
                }
            }
        }
        return result.iterator();
    }

    private Map<String, PropertySource<?>> getPropertySources() {
        Map<String, PropertySource<?>> map = new LinkedHashMap<>();
        MutablePropertySources sources;
        if (this.environment != null
                && this.environment instanceof ConfigurableEnvironment) {
            sources = this.environment.getPropertySources();
        }
        else {
            sources = new StandardEnvironment().getPropertySources();
        }
        for (PropertySource<?> source : sources) {
            extract("", map, source);
        }
        return map;
    }

    private void extract(String root, Map<String, PropertySource<?>> map,
                         PropertySource<?> source) {
        if (source instanceof CompositePropertySource) {
            for (PropertySource<?> nest : ((CompositePropertySource) source).getPropertySources()) {
                extract(source.getName() + ":", map, nest);
            }
        }
        else {
            map.put(root + source.getName(), source);
        }
    }

}
