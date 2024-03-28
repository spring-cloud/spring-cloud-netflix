package org.springframework.cloud.netflix.eureka.server.metrics;

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.Tag;

/**
 * TBD.
 *
 * @author wonchul heo
 * @since 4.1.1
 */
public interface EurekaInstanceTagProvider {

	Iterable<Tag> eurekaInstanceTags(InstanceInfo instanceInfo);
}
