package org.springframework.cloud.starter.hystrix.dashboard;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to log a warning about starter being deprecated
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class HystrixDashboardDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(HystrixDashboardDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-hystrix-dashboard is deprecated as of Spring Cloud Netflix 1.4.0, " +
		"please migrate to spring-cloud-starter-netflix-hystrix-dashboard");
	}
}
