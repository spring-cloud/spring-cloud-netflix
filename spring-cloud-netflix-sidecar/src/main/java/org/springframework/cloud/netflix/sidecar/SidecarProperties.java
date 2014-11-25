package org.springframework.cloud.netflix.sidecar;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("sidecar")
public class SidecarProperties {
    private URI healthUri;
    private URI homePageUri;
    private int port;
}
