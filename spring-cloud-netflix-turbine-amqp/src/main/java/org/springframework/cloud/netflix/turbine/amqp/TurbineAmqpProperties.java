package org.springframework.cloud.netflix.turbine.amqp;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("turbine.amqp")
@Data
public class TurbineAmqpProperties {

	private int port = 8989;

}
