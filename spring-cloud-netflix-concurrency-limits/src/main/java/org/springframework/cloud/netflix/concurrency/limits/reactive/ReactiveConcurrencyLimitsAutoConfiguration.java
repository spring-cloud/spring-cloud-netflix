/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.concurrency.limits.reactive;

import java.util.function.Consumer;

import com.netflix.concurrency.limits.Limiter;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

/**
 * Reactive autoconfiguration class for registering Netflix {@link Limiter} bean.
 *
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ WebFilter.class, Mono.class })
public class ReactiveConcurrencyLimitsAutoConfiguration {

	private final ObjectProvider<Consumer<ServerWebExchangeLimiterBuilder>> configurerProvider;

	public ReactiveConcurrencyLimitsAutoConfiguration(
			ObjectProvider<Consumer<ServerWebExchangeLimiterBuilder>> configurerProvider) {
		this.configurerProvider = configurerProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public Limiter<ServerWebExchange> webfluxLimiter() {
		ServerWebExchangeLimiterBuilder builder = new ServerWebExchangeLimiterBuilder();

		this.configurerProvider.ifAvailable(consumer -> consumer.accept(builder));

		return builder.build();
	}

	@Bean
	public ConcurrencyLimitsWebFilter concurrencyLimitsWebFilter(
			Limiter<ServerWebExchange> limiter) {
		return new ConcurrencyLimitsWebFilter(limiter);
	}

}
