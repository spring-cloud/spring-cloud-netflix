package org.springframework.cloud.netflix.eureka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * Properties used for configuring the eureka implementation of {@link org.springframework.cloud.client.discovery.DiscoveryClient}
 *
 * @author Olga Maciaszek-Sharma
 */
@ConfigurationProperties(prefix = "spring.cloud.discovery.client.eureka")
public class EurekaDiscoveryClientProperties {

	private int order = Ordered.LOWEST_PRECEDENCE;

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
