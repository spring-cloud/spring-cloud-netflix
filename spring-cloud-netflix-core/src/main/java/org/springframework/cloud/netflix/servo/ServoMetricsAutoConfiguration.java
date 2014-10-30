/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.servo;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.servo.monitor.Monitors;

/**
 * Auto configuration to configure Servo support. 
 * 
 * @author Dave Syer
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ Monitors.class, MetricReader.class })
@ConditionalOnBean(MetricReader.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
@AutoConfigureAfter({MetricRepositoryAutoConfiguration.class})
public class ServoMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ServoPublicMetrics servoPublicMetrics(MetricReader reader) {
		return new ServoPublicMetrics(reader);
	}
}
