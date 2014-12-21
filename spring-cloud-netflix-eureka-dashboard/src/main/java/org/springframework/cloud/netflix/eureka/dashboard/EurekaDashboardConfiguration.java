package org.springframework.cloud.netflix.eureka.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * @author Julien Roy
 *
 */
@Configuration
public class EurekaDashboardConfiguration {

	@Bean
	public EurekaDashboardController eurekaController() {
		return new EurekaDashboardController();
	}

}
