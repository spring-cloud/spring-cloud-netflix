package org.springframework.cloud.starter.turbine.amqp;

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
public class TurbineAmqpDeprecationWarningAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(TurbineAmqpDeprecationWarningAutoConfiguration.class);

	@PostConstruct
	public void logWarning() {
		LOGGER.warn("spring-cloud-starter-turbine-amqp is deprecated as of Spring Cloud Netflix 1.4.0, " +
				"please migrate to spring-cloud-starter-netflix-turbine-amqp");
	}
}
