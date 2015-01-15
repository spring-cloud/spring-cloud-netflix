package org.springframework.cloud.netflix.turbine.amqp;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("turbine.amqp")
@Data
public class TurbineAmqpProperties {

	@Value("${server.port:8989}")
	private int port = 8989;

}
