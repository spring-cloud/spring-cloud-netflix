/* Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.netflix.ribbon.eureka;

import org.springframework.util.StringUtils;

/**
 * Utility class for dealing with zones.
 * @author Ryan Baxter
 *
 */
public class ZoneUtils {

	/**
	 * Approximates Eureka zones from a host name. This method approximates the zone to be
	 * everything after the first "." in the host name.
	 * @param host The host name to extract the host name from
	 * @return The approximate zone
	 */
	public static String extractApproximateZone(String host) {
		if (!host.contains(".")) {
			return host;
		}
		String[] split = StringUtils.split(host, ".");
		StringBuilder builder = new StringBuilder(split[1]);
		for (int i = 2; i < split.length; i++) {
			builder.append(".").append(split[i]);
		}
		return builder.toString();
	}

}
