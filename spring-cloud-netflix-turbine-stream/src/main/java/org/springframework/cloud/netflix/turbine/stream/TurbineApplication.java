package org.springframework.cloud.netflix.turbine.stream;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableTurbineStream
public class TurbineApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(TurbineApplication.class).properties(
				"spring.config.name=turbine").run(args);
	}

}
