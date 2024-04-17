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
 * @since 3.1.6
 */
@ConfigurationProperties("eureka.client.rest-template-timeout")
public class RestTemplateTimeoutProperties {

	/**
	 * Default values are set to 180000, in keeping with {@link RequestConfig} and
	 * {@link SocketConfig} defaults.
	 */
	private int connectTimeout = 3 * 60 * 1000;

	private int connectRequestTimeout = 3 * 60 * 1000;

	private int socketTimeout = 3 * 60 * 1000;

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
