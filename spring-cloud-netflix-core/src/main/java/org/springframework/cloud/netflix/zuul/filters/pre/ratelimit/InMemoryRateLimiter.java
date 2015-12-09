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

import java.util.WeakHashMap;

/**
 * @author Vinicius Carvalho
 *
 * Loosely based on a Leaky Bucket algorithm: https://en.wikipedia.org/wiki/Leaky_bucket
 *
 * Use RedisRateLimiter for production uses.
 *
 * @see org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.redis.RedisRateLimiter
 */
public class InMemoryRateLimiter implements RateLimiter {

	WeakHashMap<String, Rate> cache = new WeakHashMap<>();


	@Override
	public synchronized Rate consume(Policy policy, String key) {
		Rate rate = cache.get(key);

		if (rate == null) {
			rate = create(policy, key);
		}
		replenish(policy, rate);
		return Rate.from(rate);
	}

	private void replenish(Policy policy, Rate rate) {
		if (rate.getReset() <= System.currentTimeMillis()) {
			rate.setRemaining(policy.getLimit());
			rate.setReset(System.currentTimeMillis() + policy.getRefreshInterval() * 1000);
		}
		rate.setRemaining(rate.getRemaining() - 1);
	}

	private Rate create(Policy policy, String key) {
		Rate rate = new Rate(policy.getLimit(), policy.getLimit(), (System.currentTimeMillis() + policy.getRefreshInterval() * 1000));
		cache.put(key, rate);
		return rate;
	}

}
