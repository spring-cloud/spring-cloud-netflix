/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.hystrix;

import org.reactivestreams.Publisher;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

/**
 * Utility class to wrap a {@see Publisher} in a {@see HystrixObservableCommand}. Good for
 * use in a Spring WebFlux application. Allows more flexibility than the @HystrixCommand
 * annotation.
 * @author Spencer Gibb
 */
public class HystrixCommands {

	public static <T> Flux<T> wrap(String commandName, Flux<T> flux) {
		return wrap(commandName, flux, null);
	}

	public static <T> Flux<T> wrap(String commandName, Flux<T> flux, Flux<T> fallback) {
		String groupName = commandName + "group";
		PublisherHystrixCommand<T> command = createHystrixCommand(commandName, groupName,
				flux, fallback);
		return Flux.from(RxReactiveStreams.toPublisher(command.toObservable()));
	}

	public static <T> Mono<T> wrap(String commandName, Mono<T> mono) {
		return wrap(commandName, mono, null);
	}

	public static <T> Mono<T> wrap(String commandName, Mono<T> mono, Mono<T> fallback) {
		String groupName = commandName + "group";
		PublisherHystrixCommand<T> command = createHystrixCommand(commandName, groupName,
				mono, fallback);
		return Mono.from(RxReactiveStreams.toPublisher(command.toObservable()));
	}

	private static <T> PublisherHystrixCommand<T> createHystrixCommand(String commandName,
			String groupName, Publisher<T> publisher, Publisher<T> fallback) {
		HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(groupName);
		HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);

		HystrixObservableCommand.Setter setter = HystrixObservableCommand.Setter
				.withGroupKey(groupKey).andCommandKey(commandKey);

		return new PublisherHystrixCommand<>(setter, publisher, fallback);
	}

	private static class PublisherHystrixCommand<T> extends HystrixObservableCommand<T> {

		private Publisher<T> publisher;
		private Publisher<T> fallback;

		protected PublisherHystrixCommand(Setter setter, Publisher<T> publisher,
				Publisher<T> fallback) {
			super(setter);
			this.publisher = publisher;
			this.fallback = fallback;
		}

		@Override
		protected Observable<T> construct() {
			return RxReactiveStreams.toObservable(publisher);
		}

		@Override
		protected Observable<T> resumeWithFallback() {
			if (this.fallback != null) {
				return RxReactiveStreams.toObservable(this.fallback);
			}
			return super.resumeWithFallback();
		}
	}
}
