package org.springframework.cloud.starter.eureka;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to print warning message about Eureka starter deprecation
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class EurekaStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(EurekaStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-eureka is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-eureka");
	}
}
