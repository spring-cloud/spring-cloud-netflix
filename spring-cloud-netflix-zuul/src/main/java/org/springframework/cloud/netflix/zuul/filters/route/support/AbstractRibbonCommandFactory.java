/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;

/**
 * @author Ryan Baxter
 */
public abstract class AbstractRibbonCommandFactory implements RibbonCommandFactory {

	private Map<String, ZuulFallbackProvider> fallbackProviderCache;
	private ZuulFallbackProvider defaultFallbackProvider = null;

	public AbstractRibbonCommandFactory(Set<ZuulFallbackProvider> fallbackProviders){
		this.fallbackProviderCache = new HashMap<>();
		for(ZuulFallbackProvider provider : fallbackProviders) {
			String route = provider.getRoute();
			if("*".equals(route) || route == null) {
				defaultFallbackProvider = provider;
			} else {
				fallbackProviderCache.put(route, provider);
			}
		}
	}

	protected ZuulFallbackProvider getFallbackProvider(String route) {
		ZuulFallbackProvider provider = fallbackProviderCache.get(route);
		if(provider == null) {
			provider = defaultFallbackProvider;
		}
		return provider;
	}
}
