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

package org.springframework.cloud.netflix.eureka;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

/**
 * Poll the Eureka server periodically, triggering a EurekaStatusChangedEvent when the InstanceStatus changes from the
 * server side.
 *
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnBean(EurekaClient.class)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class EurekaApplicationEventAutoConfiguration implements ApplicationEventPublisherAware {
	private InstanceInfo.InstanceStatus currentStatus = InstanceInfo.InstanceStatus.UNKNOWN;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	@Value("${eureka.remoteStatus.polling.interval:3000}")
	Integer pollingInterval;

	@Value("${eureka.remoteStatus.polling.enabled:true}")
	Boolean enabled;

	@Autowired
	EurekaClient eurekaClient;

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		if(enabled) {
			executor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					InstanceInfo.InstanceStatus latestStatus = eurekaClient.getInstanceRemoteStatus();
					if (!latestStatus.equals(currentStatus)) {
						EurekaApplicationEventAutoConfiguration.this.currentStatus = latestStatus;
						publisher.publishEvent(new EurekaStatusChangedEvent(latestStatus));
					}
				}
			}, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
		}
	}
}
