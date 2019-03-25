/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("hystrix.stream.queue")
public class HystrixStreamProperties {

	/** Flag to indicate that Hystrix Stream is enabled. Default is true. */
	private boolean enabled = true;

	/** Flag to indicate to prefix metric names with serviceId. Default is true. */
	private boolean prefixMetricName = true;

	/** Flag to indicate to send the id field in the metrics. Default is true */
	private boolean sendId = true;

	/**
	 * The destination of the stream. Destination as defined by Spring Cloud Stream.
	 * Defaults to springCloudHystrixStream
	 */
	private String destination = HystrixConstants.HYSTRIX_STREAM_DESTINATION;

	/** The content type of the messages. Defaults to application/json */
	private String contentType = "application/json";

	/** How often (in ms) to send messages to the stream. Defaults to 500. */
	private long sendRate = 500;

	/**
	 * How often to put messages in the queue. This queue drains to the stream. Defaults
	 * to 500.
	 */
	private long gatherRate = 500;

	/**
	 * The size of the metrics queue. This queue drains to the stream. Defaults to 1000.
	 */
	private int size = 1000;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPrefixMetricName() {
		return prefixMetricName;
	}

	public void setPrefixMetricName(boolean prefixMetricName) {
		this.prefixMetricName = prefixMetricName;
	}

	public boolean isSendId() {
		return sendId;
	}

	public void setSendId(boolean sendId) {
		this.sendId = sendId;
	}

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

	public long getSendRate() {
		return sendRate;
	}

	public void setSendRate(long sendRate) {
		this.sendRate = sendRate;
	}

	public long getGatherRate() {
		return gatherRate;
	}

	public void setGatherRate(long gatherRate) {
		this.gatherRate = gatherRate;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
