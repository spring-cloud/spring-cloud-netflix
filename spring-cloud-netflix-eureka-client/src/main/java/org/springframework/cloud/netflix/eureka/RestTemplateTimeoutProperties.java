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

import java.util.Objects;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.web.client.RestTemplate;

/**
 * Properties for configuring timeouts used in {@link RestTemplate} required by
 * {@link RestTemplateEurekaHttpClient}.
 *
 * @author Jiwon Jeon
 * @author Mooyong Lee
 * @since 3.1.6
 */
@ConfigurationProperties("eureka.client.rest-template-timeout")
public class RestTemplateTimeoutProperties extends TimeoutProperties {

	@Override
	public String toString() {
		return "RestTemplateTimeoutProperties{" + ", connectTimeout=" + connectTimeout + ", connectRequestTimeout="
				+ connectRequestTimeout + ", socketTimeout=" + socketTimeout + '}';
	}
}
