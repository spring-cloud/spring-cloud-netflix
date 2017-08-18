package org.springframework.cloud.starter.zuul;

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
public class ZuulDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(ZuulDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-zuul is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-zuul");
	}
}
