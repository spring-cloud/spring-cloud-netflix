/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.concurrency.limits.reactive;

import com.netflix.concurrency.limits.Limiter;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

public class ConcurrencyLimitsWebFilter implements WebFilter {

	private final Limiter<ServerWebExchange> limiter;

	public ConcurrencyLimitsWebFilter(Limiter<ServerWebExchange> limiter) {
		this.limiter = limiter;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return limiter.acquire(exchange)
				.map(listener -> chain.filter(exchange)
						.doOnSuccess(v -> listener.onSuccess())
						.doOnError(throwable -> listener.onIgnore()))
				.orElseGet(() -> {
					exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
					//TODO: set body
					return exchange.getResponse().setComplete();
				});
	}
}
