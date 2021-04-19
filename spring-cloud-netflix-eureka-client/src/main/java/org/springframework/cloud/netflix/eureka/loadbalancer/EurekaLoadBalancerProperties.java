/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.loadbalancer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A {@link ConfigurationProperties} bean for the Eureka-specific instrumentation of
 * Spring Cloud LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
@ConfigurationProperties("spring.cloud.loadbalancer.eureka")
public class EurekaLoadBalancerProperties {

	/**
	 * Used to determine whether we should try to get the `zone` value from host name.
	 */
	private boolean approximateZoneFromHostname = false;

	public boolean isApproximateZoneFromHostname() {
		return approximateZoneFromHostname;
	}

	public void setApproximateZoneFromHostname(boolean approximateZoneFromHostname) {
		this.approximateZoneFromHostname = approximateZoneFromHostname;
	}

}
