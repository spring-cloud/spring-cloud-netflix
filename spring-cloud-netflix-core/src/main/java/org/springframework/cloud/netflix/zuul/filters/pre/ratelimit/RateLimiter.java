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

/**
 * @author Vinicius Carvalho
 */
public interface RateLimiter {

	/**
	 *
	 * @param policy - Template for which rates should be created in case there's no rate limit associated with the key
	 * @param key - Unique key that identifies a request
	 * @return a view of a user's rate request limit
	 */
	public Rate consume(Policy policy, String key);
}
