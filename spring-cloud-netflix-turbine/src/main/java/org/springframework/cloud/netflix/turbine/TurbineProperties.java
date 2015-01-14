package org.springframework.cloud.netflix.turbine;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("turbine")
public class TurbineProperties {
	private String clusterNameExpression = "appName";
}
