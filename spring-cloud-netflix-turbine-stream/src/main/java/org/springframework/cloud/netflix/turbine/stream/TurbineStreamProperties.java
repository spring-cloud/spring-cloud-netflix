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

package org.springframework.cloud.netflix.turbine.stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("turbine.stream") public class TurbineStreamProperties {

	@Value("${server.port:8989}")
	private int port = 8989;

	private String destination = HystrixConstants.HYSTRIX_STREAM_DESTINATION;

	public TurbineStreamProperties() {
	}

	public int getPort() {
		return this.port;
	}

	public String getDestination() {
		return this.destination;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof TurbineStreamProperties))
			return false;
		final TurbineStreamProperties other = (TurbineStreamProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		if (this.port != other.port)
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
		result = result * PRIME + this.port;
		final Object $destination = this.destination;
		result = result * PRIME + ($destination == null ? 0 : $destination.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof TurbineStreamProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.turbine.stream.TurbineStreamProperties(port="
				+ this.port + ", destination=" + this.destination + ")";
	}
}
