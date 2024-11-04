package org.springframework.cloud.netflix.eureka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Olga Maciaszek-Sharma
 */
@ConfigurationProperties("eureka.client.restclient.timeout")
public class RestClientTimeoutProperties extends TimeoutProperties {

	@Override
	public String toString() {
		return "RestClientTimeoutProperties{" + ", connectTimeout=" + connectTimeout + ", connectRequestTimeout="
				+ connectRequestTimeout + ", socketTimeout=" + socketTimeout + '}';
	}
}
