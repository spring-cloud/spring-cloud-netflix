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

package org.springframework.cloud.netflix.eureka.config;

import java.util.List;

import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class HostnameBasedUrlRandomizer implements EndpointUtils.ServiceUrlRandomizer {

	private final String hostname;

	HostnameBasedUrlRandomizer(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public void randomize(List<String> urlList) {
		int listSize = 0;
		if (urlList != null) {
			listSize = urlList.size();
		}
		if (!StringUtils.hasText(hostname) || listSize == 0) {
			return;
		}
		// Find the hashcode of the instance hostname and use it to find an entry
		// and then arrange the rest of the entries after this entry.
		int instanceHashcode = hostname.hashCode();
		if (instanceHashcode < 0) {
			instanceHashcode = instanceHashcode * -1;
		}
		int backupInstance = instanceHashcode % listSize;
		for (int i = 0; i < backupInstance; i++) {
			String zone = urlList.remove(0);
			urlList.add(zone);
		}
	}

	public static String getEurekaUrl(EurekaClientConfig config, String hostname) {
		List<String> urls = EndpointUtils.getDiscoveryServiceUrls(config, EurekaClientConfigBean.DEFAULT_ZONE,
				new HostnameBasedUrlRandomizer(hostname));
		return urls.get(0);
	}

	public static DefaultEndpoint randomEndpoint(EurekaClientConfig config, Environment env) {
		String hostname = env.getProperty("eureka.instance.hostname");
		return new DefaultEndpoint(getEurekaUrl(config, hostname));
	}

	public static DefaultEndpoint randomEndpoint(EurekaClientConfig config, Binder binder) {
		String hostname = binder.bind("eureka.instance.hostname", String.class).orElseGet(() -> null);
		return new DefaultEndpoint(getEurekaUrl(config, hostname));
	}

}
