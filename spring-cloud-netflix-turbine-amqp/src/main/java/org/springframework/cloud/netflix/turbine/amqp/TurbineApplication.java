package org.springframework.cloud.netflix.turbine.amqp;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableTurbineAmqp
public class TurbineApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(TurbineApplication.class).properties(
				"spring.config.name=turbine").run(args);
	}

}
