package org.springframework.cloud.starter.turbine.stream;

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
public class TurbineStreamDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(TurbineStreamDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-turbine-stream is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-turbine-stream");
	}
}
