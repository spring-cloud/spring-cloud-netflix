package org.springframework.cloud.netflix.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;

/**
 * @author Olga Maciaszek-Sharma
 * @since 2.1.0
 * @deprecated Module spring-cloud-netflix-core is deprecated as of 2.1.0, use spring-cloud-netflix-hystrix instead.
 */
@Configuration
@Deprecated
public class CoreAutoConfiguration {

    private static final Log LOG = LogFactory.getLog(CoreAutoConfiguration.class);

    public CoreAutoConfiguration() {
        LOG.warn("This module is deprecated. It will be removed in the next major release. " +
                "Please use spring-cloud-netflix-hystrix instead.");
    }
}
