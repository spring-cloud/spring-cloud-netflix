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

/**
 * Implementation of {@link RestTemplateTimeoutProperties}.
 *
 * @author Jiwon Jeon
 * @since 4.0.0
 */
public class DefaultRestTemplateTimeoutProperties implements RestTemplateTimeoutProperties {

	private int connectTimeout = 10 * 1000;

	private int connectRequestTimeout = 10 * 1000;

	private int socketTimeout = 10 * 1000;

	@Override
	public int getConnectTimeout() {
		return connectTimeout;
	}

	@Override
	public int getConnectRequestTimeout() {
		return connectRequestTimeout;
	}

	@Override
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

		DefaultRestTemplateTimeoutProperties that = (DefaultRestTemplateTimeoutProperties) o;

		return connectTimeout == that.connectTimeout && connectRequestTimeout == that.connectRequestTimeout
				&& socketTimeout == that.socketTimeout;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectTimeout, connectRequestTimeout, socketTimeout);
	}

	@Override
	public String toString() {
		return "DefaultRestTemplateConfig{" + ", connectTimeout=" + connectTimeout + ", connectRequestTimeout="
				+ connectRequestTimeout + ", socketTimeout=" + socketTimeout + '}';
	}

}
