package org.springframework.cloud.netflix.eureka.server.metrics;

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * TBD.
 *
 * @author wonchul heo
 * @since 4.1.1
 */
class DefaultEurekaInstanceTagProvider implements EurekaInstanceTagProvider {
	@Override
	public Iterable<Tag> eurekaInstanceTags(InstanceInfo instanceInfo) {
		return Tags.of(
				Tag.of("application", instanceInfo.getAppName()),
				Tag.of("status", instanceInfo.getStatus().name()));
	}
}
