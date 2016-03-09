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

package org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.redis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.Policy;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.Rate;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimiter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

/**
 * @author Vinicius Carvalho
 */
public class RedisRateLimiter implements RateLimiter {
	private RedisTemplate template;

	public RedisRateLimiter(RedisTemplate template) {
		this.template = template;
	}

	@Override
	public Rate consume(final Policy policy, final String key) {
		Long now = System.currentTimeMillis();
		Long time = (Long) (now / (1000 * policy.getRefreshInterval()));
		final String tempKey = key + ":" + time;
		List results = (List) template.execute(new SessionCallback() {

			@Override
			public List<Object> execute(RedisOperations ops) throws DataAccessException {
				ops.multi();
				ops.boundValueOps(tempKey).increment(1L);
				ops.expire(tempKey, policy.getRefreshInterval(), TimeUnit.SECONDS);

				return ops.exec();
			}
		});
		Long current = (Long) results.get(0);
		return new Rate(policy.getLimit(), Math.max(0, policy.getLimit() - current), time * (policy.getRefreshInterval() * 1000));
	}
}
