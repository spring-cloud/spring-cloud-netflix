package org.springframework.cloud.netflix.zuul;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("zuul.proxy")
public class ZuulProxyProperties {
    private String mapping = "/proxy";
    private boolean stripMapping = true; // this is currently the default behaviour
}
