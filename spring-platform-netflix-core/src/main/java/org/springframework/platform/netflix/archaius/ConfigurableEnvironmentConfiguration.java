package org.springframework.platform.netflix.archaius;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by sgibb on 6/27/14.
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
        Map<String, PropertySource<?>> map = new LinkedHashMap<String, PropertySource<?>>();
        MutablePropertySources sources = null;
        if (this.environment != null
                && this.environment instanceof ConfigurableEnvironment) {
            sources = ((ConfigurableEnvironment) this.environment).getPropertySources();
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
            Set<PropertySource<?>> nested = getNestedPropertySources((CompositePropertySource) source);
            for (PropertySource<?> nest : nested) {
                extract(source.getName() + ":", map, nest);
            }
        }
        else {
            map.put(root + source.getName(), source);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<PropertySource<?>> getNestedPropertySources(CompositePropertySource source) {
        try {
            Field field = ReflectionUtils.findField(CompositePropertySource.class,
                    "propertySources");
            field.setAccessible(true);
            return (Set<PropertySource<?>>) field.get(source);
        }
        catch (Exception ex) {
            return Collections.emptySet();
        }
    }
}
