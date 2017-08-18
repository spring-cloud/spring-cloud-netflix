package org.springframework.cloud.starter.spectator;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to print deprecation warning for starter
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class SpectatorDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(SpectatorDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-spectator is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-spectator");
	}
}
