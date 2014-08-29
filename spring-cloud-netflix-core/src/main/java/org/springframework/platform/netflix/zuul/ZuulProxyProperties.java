package org.springframework.platform.netflix.zuul;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by sgibb on 8/5/14.
 */
@Data
@ConfigurationProperties("zuul.proxy")
public class ZuulProxyProperties {
    private String mapping = "/proxy";
}
