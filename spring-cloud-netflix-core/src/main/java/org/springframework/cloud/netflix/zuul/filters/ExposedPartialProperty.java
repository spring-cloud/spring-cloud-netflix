package org.springframework.cloud.netflix.zuul.filters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration that allows to expose endpoints of a route
 *
 * @author Kevin Van Houtte
 */
@ConfigurationProperties(prefix = "partial-exposed")
@Component
public class ExposedPartialProperty {

    private Map<String, List<String>> paths = new LinkedHashMap<>();

    public Map<String, List<String>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, List<String>> paths) {
        this.paths = paths;
    }
}
