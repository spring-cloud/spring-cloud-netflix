package org.springframework.netflix.turbine.amqp;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.turbine.amqp.EnableTurbineAmqp;

/**
 * @author Spencer Gibb
 */
@EnableAutoConfiguration
@EnableTurbineAmqp
public class SampleTurbineAmqpApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder()
				.sources(SampleTurbineAmqpApplication.class)
				.run(args);
	}
}
