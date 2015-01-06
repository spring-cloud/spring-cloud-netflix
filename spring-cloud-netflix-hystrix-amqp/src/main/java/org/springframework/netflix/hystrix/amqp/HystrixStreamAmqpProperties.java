package org.springframework.netflix.hystrix.amqp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("hystrix.stream.amqp")
@Data
public class HystrixStreamAmqpProperties {
	private boolean enabled = true;
	private boolean prefixMetricName = true;
	private boolean sendId = true;
}
