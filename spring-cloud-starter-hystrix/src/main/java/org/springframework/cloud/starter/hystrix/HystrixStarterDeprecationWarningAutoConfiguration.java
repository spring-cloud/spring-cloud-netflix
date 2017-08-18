package org.springframework.cloud.starter.hystrix;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration class to print deprecation warning for starter
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class HystrixStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(HystrixStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-hystrix is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-hystrix");
	}
}
