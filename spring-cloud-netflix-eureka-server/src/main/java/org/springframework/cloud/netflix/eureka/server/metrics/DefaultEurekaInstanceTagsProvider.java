/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.server.metrics;

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation for {@link EurekaInstanceTagsProvider}.
 *
 * @author Wonchul Heo
 * @since 4.1.2
 */
class DefaultEurekaInstanceTagsProvider implements EurekaInstanceTagsProvider {

	@Override
	public Tags eurekaInstanceTags(InstanceInfo instanceInfo) {
		return Tags.of(Tag.of("application", instanceInfo.getAppName()),
				Tag.of("status", instanceInfo.getStatus().name()));
	}

}
