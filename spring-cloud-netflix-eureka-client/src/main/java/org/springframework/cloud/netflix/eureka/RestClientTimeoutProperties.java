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

package org.springframework.cloud.netflix.eureka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.client.RestClient;

/**
 * A {@link RestClient}-specific {@link TimeoutProperties} implementation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
@ConfigurationProperties("eureka.client.restclient.timeout")
public class RestClientTimeoutProperties extends TimeoutProperties {

	@Override
	public String toString() {
		return "RestClientTimeoutProperties{" + ", connectTimeout=" + connectTimeout + ", connectRequestTimeout="
				+ connectRequestTimeout + ", socketTimeout=" + socketTimeout + '}';
	}

}
