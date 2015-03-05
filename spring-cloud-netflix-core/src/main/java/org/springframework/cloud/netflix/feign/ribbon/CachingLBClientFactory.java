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

package org.springframework.cloud.netflix.feign.ribbon;

import feign.ribbon.LBClient;
import feign.ribbon.LBClientFactory;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

/**
 * LBClientFactory that caches entries created.
 * @author Spencer Gibb
 */
public class CachingLBClientFactory implements LBClientFactory {

	private volatile Map<String, LBClient> cache = new ConcurrentReferenceHashMap<>();
	private final LBClientFactory delegate;

	public CachingLBClientFactory(LBClientFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public LBClient create(String clientName) {
		if (cache.containsKey(clientName)) {
			return cache.get(clientName);
		}
		LBClient client = delegate.create(clientName);
		cache.put(clientName, client);
		return client;
	}
}
