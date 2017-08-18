package org.springframework.cloud.starter.feign;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to log warning about deprecation of starter
 * @author Ryan Baxter
 */
@Configuration
@Deprecated
public class FeignStarterDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(FeignStarterDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-feign is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-openfeign");
	}
}
