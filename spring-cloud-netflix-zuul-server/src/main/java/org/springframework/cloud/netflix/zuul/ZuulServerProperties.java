package org.springframework.cloud.netflix.zuul;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("zuul.server")
public class ZuulServerProperties {
    private String mapping = "";
    private boolean stripMapping = false;
    private String routePrefix = "zuul.server.route.";
    private boolean addProxyHeaders = true;
}
