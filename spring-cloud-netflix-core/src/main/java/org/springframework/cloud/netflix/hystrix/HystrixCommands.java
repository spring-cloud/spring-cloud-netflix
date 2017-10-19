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
import org.springframework.util.StringUtils;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.HystrixObservableCommand.Setter;

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

	public static <T> PublisherBuilder<T> from(Publisher<T> publisher) {
		return new PublisherBuilder<>(publisher);
	}

	public static class PublisherBuilder<T> {
		private final Publisher<T> publisher;
		private String commandName;
		private String groupName;
		private Publisher<T> fallback;
		private Setter setter;
		private boolean eager = false;

		public PublisherBuilder(Publisher<T> publisher) {
			this.publisher = publisher;
		}

		public PublisherBuilder<T> commandName(String commandName) {
			this.commandName = commandName;
			return this;
		}

		public PublisherBuilder<T> groupName(String groupName) {
			this.groupName = groupName;
			return this;
		}

		public PublisherBuilder<T> fallback(Publisher<T> fallback) {
			this.fallback = fallback;
			return this;
		}

		public PublisherBuilder<T> setter(Setter setter) {
			this.setter = setter;
			return this;
		}

		public PublisherBuilder<T> eager() {
			this.eager = true;
			return this;
		}

		public Publisher<T> build() {
			if (!StringUtils.hasText(commandName) && setter == null) {
				throw new IllegalStateException("commandName and setter can not both be empty");
			}
			Setter setterToUse;
			if (this.setter != null) {
				setterToUse = this.setter;
			} else {
				String groupNameToUse;
				if (StringUtils.hasText(this.groupName)) {
					groupNameToUse = this.groupName;
				} else {
					groupNameToUse = commandName + "group";
				}

				HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(groupNameToUse);
				HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(this.commandName);
				setterToUse = Setter.withGroupKey(groupKey).andCommandKey(commandKey);
			}

			PublisherHystrixCommand<T> command = new PublisherHystrixCommand<>(setterToUse, this.publisher, this.fallback);

			Observable<T> observable;
			if (this.eager) {
				observable = command.observe();
			} else {
				observable = command.toObservable();
			}
			return RxReactiveStreams.toPublisher(observable);
		}

		public Flux<T> toFlux() {
			return Flux.from(build());
		}

		public Mono<T> toMono() {
			return Mono.from(build());
		}

	}

	public static <T> Mono<T> inject(String commandName, Mono<T> mono, Mono<T> fallback, boolean eager) {
		String groupName = commandName + "group";
		PublisherHystrixCommand<T> command = createHystrixCommand(commandName, groupName,
				mono, fallback);
		Observable<T> observable;
		if (eager) {
			observable = command.observe();
		} else {
			observable = command.toObservable();
		}
		return Mono.from(RxReactiveStreams.toPublisher(observable));
	}

	private static <T> PublisherHystrixCommand<T> createHystrixCommand(String commandName,
			String groupName, Publisher<T> publisher, Publisher<T> fallback) {
		HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(groupName);
		HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);

		Setter setter = Setter
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
