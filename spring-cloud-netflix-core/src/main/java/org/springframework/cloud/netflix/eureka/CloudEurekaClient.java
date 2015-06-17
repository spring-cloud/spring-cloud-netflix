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

package org.springframework.cloud.netflix.eureka;

import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.BeansException;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class CloudEurekaClient extends DiscoveryClient implements ApplicationContextAware {

	private AtomicLong cacheRefreshedCount = new AtomicLong(0);

	private ApplicationContext context;

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig config) {
		super(applicationInfoManager, config);
	}

	@Override
	protected void onCacheRefreshed() {
		long newCount = cacheRefreshedCount.incrementAndGet();
		log.trace("onCacheRefreshed called with count: " + newCount);
		this.context.publishEvent(new HeartbeatEvent(this, newCount));
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
