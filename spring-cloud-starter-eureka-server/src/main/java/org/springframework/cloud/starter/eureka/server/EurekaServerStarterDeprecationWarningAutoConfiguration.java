package org.springframework.cloud.starter.eureka.server;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to print deprecation warning for spring-cloud-starter-eureka-server
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class EurekaServerStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(EurekaServerStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-eureka-server is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-eureka-server");
	}
}
