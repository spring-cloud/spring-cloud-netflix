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

import java.security.Principal;
import java.util.Optional;
import java.util.function.Function;

import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;

import org.springframework.web.server.ServerWebExchange;

public class ServerWebExchangeLimiterBuilder extends AbstractPartitionedLimiter.Builder<ServerWebExchangeLimiterBuilder, ServerWebExchange> {
	/**
	 * Partition the limit by header
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByHeader(String name) {
		return partitionResolver(exchange -> exchange.getRequest().getHeaders().getFirst(name));
	}

	/**
	 * Partition the limit by {@link Principal}. Percentages of the limit are partitioned to named
	 * groups.  Group membership is derived from the provided mapping function.
	 * @param principalToGroup Mapping function from {@link Principal} to a named group.
	 * @param configurer Configuration function though which group percentages may be specified
	 *                   Unspecified group values may only use excess capacity.
	 * @return Chainable builder
	 */
	/*public ServerWebExchangeLimiterBuilder partitionByUserPrincipal(Function<Principal, String> principalToGroup, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionResolver(
				exchange -> Optional.ofNullable(request.getUserPrincipal()).map(principalToGroup).orElse(null),
				configurer);
	}*/

	/**
	 * Partition the limit by request attribute
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByAttribute(String name) {
		return partitionResolver( exchange -> exchange.getAttribute(name));
	}

	/**
	 * Partition the limit by request parameter
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByParameter(String name) {
		return partitionResolver(exchange -> exchange.getRequest().getQueryParams().getFirst(name));
	}

	/**
	 * Partition the limit by the full path. Percentages of the limit are partitioned to named
	 * groups.  Group membership is derived from the provided mapping function.
	 * @param pathToGroup Mapping function from full path to a named group.
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByPathInfo(Function<String, String> pathToGroup) {
		return partitionResolver(
				exchange -> {
					//TODO: pathWithinApplication?
					String path = exchange.getRequest().getPath().contextPath().value();
					return Optional.ofNullable(path).map(pathToGroup).orElse(null);
				});
	}

	@Override
	protected ServerWebExchangeLimiterBuilder self() {
		return this;
	}

}
