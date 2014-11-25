package org.springframework.cloud.netflix.zuul;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {
    private String mapping = "";
    private boolean stripMapping = false;
    private String routePrefix = "zuul.route.";
    private boolean addProxyHeaders = true;
    private List<String> ignoredServices = Collections.emptyList();
}
