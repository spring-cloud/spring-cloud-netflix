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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.redis.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Vinicius Carvalho
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty("zuul.ratelimit.enabled")

public class RateLimitAutoConfiguration {

	@Bean
	public RateLimitFilter rateLimiterFilter(RateLimiter rateLimiter, RateLimitProperties rateLimitProperties){
		return new RateLimitFilter(rateLimiter,rateLimitProperties);
	}

	@ConditionalOnMissingBean(name = {"redisTemplate"})
	static class InMemoryRateLimitConfiguration {

		@Bean
		public RateLimiter inMemoryRateLimiter() {
			InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();
			return rateLimiter;
		}

	}


	@ConditionalOnBean(name = {"redisTemplate"})
	@ConditionalOnClass(RedisTemplate.class)
	static class RedisRateLimiterConfiguration {

		@Bean
		public RateLimiter redisRateLimiter(RedisTemplate redisTemplate) {
			RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate);
			return rateLimiter;
		}
	}

}
