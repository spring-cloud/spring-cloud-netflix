package org.springframework.cloud.netflix.sidecar;

import java.net.URI;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
