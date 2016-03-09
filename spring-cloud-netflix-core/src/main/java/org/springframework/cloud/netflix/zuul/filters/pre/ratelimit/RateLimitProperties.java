/*
 *  Copyright 2013-2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.pre.ratelimit;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;


import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vinicius Carvalho
 */
@ConfigurationProperties("zuul.ratelimit")
public class RateLimitProperties {

	private Map<String, Policy> policies = new LinkedHashMap<>();

	private boolean enabled;

	@PostConstruct
	public void init() {
		if (policies.get(Policy.PolicyType.ANONYMOUS) == null) {
			policies.put(Policy.PolicyType.ANONYMOUS.toString(), new Policy(60L, 60L));
		}
	}

	public Map<String, Policy> getPolicies() {
		return policies;
	}

	public void setPolicies(Map<String, Policy> policies) {
		this.policies = policies;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
