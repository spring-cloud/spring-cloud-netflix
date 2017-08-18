package org.springframework.cloud.starter.ribbon;

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
public class RibbonDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(RibbonDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-ribbon is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-ribbon");
	}
}
