/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("hystrix.stream.queue") public class HystrixStreamProperties {

	private boolean enabled = true;

	private boolean prefixMetricName = true;

	private boolean sendId = true;

	private String destination = HystrixConstants.HYSTRIX_STREAM_DESTINATION;

	public HystrixStreamProperties() {
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isPrefixMetricName() {
		return this.prefixMetricName;
	}

	public boolean isSendId() {
		return this.sendId;
	}

	public String getDestination() {
		return this.destination;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setPrefixMetricName(boolean prefixMetricName) {
		this.prefixMetricName = prefixMetricName;
	}

	public void setSendId(boolean sendId) {
		this.sendId = sendId;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof HystrixStreamProperties))
			return false;
		final HystrixStreamProperties other = (HystrixStreamProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		if (this.enabled != other.enabled)
			return false;
		if (this.prefixMetricName != other.prefixMetricName)
			return false;
		if (this.sendId != other.sendId)
			return false;
		final Object this$destination = this.destination;
		final Object other$destination = other.destination;
		if (this$destination == null ?
				other$destination != null :
				!this$destination.equals(other$destination))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + (this.enabled ? 79 : 97);
		result = result * PRIME + (this.prefixMetricName ? 79 : 97);
		result = result * PRIME + (this.sendId ? 79 : 97);
		final Object $destination = this.destination;
		result = result * PRIME + ($destination == null ? 0 : $destination.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof HystrixStreamProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.hystrix.stream.HystrixStreamProperties(enabled="
				+ this.enabled + ", prefixMetricName=" + this.prefixMetricName
				+ ", sendId=" + this.sendId + ", destination=" + this.destination + ")";
	}
}
