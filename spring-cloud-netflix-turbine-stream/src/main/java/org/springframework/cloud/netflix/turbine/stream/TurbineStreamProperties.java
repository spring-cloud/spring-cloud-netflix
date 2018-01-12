/*
 * Copyright 2013-2017 the original author or authors.
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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;
import org.springframework.http.MediaType;

/**
 * @author Dave Syer
 * @author Gregor Zurowski
 */
@ConfigurationProperties("turbine.stream")
public class TurbineStreamProperties {

	private String destination = HystrixConstants.HYSTRIX_STREAM_DESTINATION;

	private String contentType = MediaType.APPLICATION_JSON_VALUE;

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TurbineStreamProperties that = (TurbineStreamProperties) o;
		return Objects.equals(destination, that.destination)
				&& Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(destination, contentType);
	}

	@Override
	public String toString() {
		return new StringBuilder("TurbineStreamProperties{")
				.append(", ").append("destination='").append(destination).append("', ")
				.append("contentType='").append(contentType).append("'}").toString();
	}

}
