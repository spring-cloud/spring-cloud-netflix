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
 *
 * Represents a view of rate limit in a giving time for a user.
 *
 * limit - How many requests can be executed by the user. Maps to X-RateLimit-Limit header
 * remaining - How many requests are still left on the current window. Maps to X-RateLimit-Remaining header
 * reset - Epoch when the rate is replenished by limit. Maps to X-RateLimit-Reset header
 */
public class Rate {
	private Long limit;

	private Long remaining;

	private Long reset;

	public Rate(Long limit, Long remaining, Long reset) {
		this.limit = limit;
		this.remaining = remaining;
		this.reset = reset;
	}

	public static Rate from(Rate original) {
		return new Rate(original.limit, original.remaining, original.reset);
	}


	public Long getLimit() {
		return limit;
	}

	public void setLimit(Long limit) {
		this.limit = limit;
	}

	public Long getRemaining() {
		return remaining;
	}

	public void setRemaining(Long remaining) {
		this.remaining = remaining;
	}

	public Long getReset() {
		return reset;
	}

	public void setReset(Long reset) {
		this.reset = reset;
	}
}
