package org.springframework.cloud.starter.atlas;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to print warning about atlas starter deprecation.
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class AtlasStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(AtlasStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-atlas is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-atlas");
	}
}
