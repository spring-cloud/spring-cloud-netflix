/*
 * Copyright 2013-2022 the original author or authors.
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

import org.apache.http.client.config.RequestConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.web.client.RestTemplate;

/**
 * Properties for configuring timeouts used in {@link RestTemplate} required by
 * {@link RestTemplateEurekaHttpClient}.
 *
 * @author Jiwon Jeon
 * @since 4.0.0
 */
@ConfigurationProperties("eureka.client.rest-template-timeout")
public class RestTemplateTimeoutProperties {

	/**
	 * Default values are set to -1 according to {@link RequestConfig .DEFAULT}.
	 */
	private int connectTimeout = -1;

	private int connectRequestTimeout = -1;

	private int socketTimeout = -1;

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public int getConnectRequestTimeout() {
		return connectRequestTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setConnectRequestTimeout(int connectRequestTimeout) {
		this.connectRequestTimeout = connectRequestTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		RestTemplateTimeoutProperties that = (RestTemplateTimeoutProperties) o;

		return connectTimeout == that.connectTimeout && connectRequestTimeout == that.connectRequestTimeout
				&& socketTimeout == that.socketTimeout;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectTimeout, connectRequestTimeout, socketTimeout);
	}

	@Override
	public String toString() {
		return "RestTemplateTimeoutProperties{" + ", connectTimeout=" + connectTimeout + ", connectRequestTimeout="
				+ connectRequestTimeout + ", socketTimeout=" + socketTimeout + '}';
	}

}
