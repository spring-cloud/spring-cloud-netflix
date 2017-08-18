package org.springframework.cloud.starter.archaius;

/**
 * Auto configuration to print deprecation warning about starter.
 * @author Ryan Baxter
 */

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@Deprecated
public class ArchaiusStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(ArchaiusStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-archaius is deprecated as of Spring Cloud Netflix 1.4.0, " +
		"please migrate to spring-cloud-starter-netflix-archaius");
	}
}
