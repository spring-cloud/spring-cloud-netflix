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

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("hystrix.stream.queue")
@Data
public class HystrixStreamProperties {

	private boolean enabled = true;

	private boolean prefixMetricName = true;

	private boolean sendId = true;

	private String destination = HystrixConstants.HYSTRIX_STREAM_DESTINATION;

}
