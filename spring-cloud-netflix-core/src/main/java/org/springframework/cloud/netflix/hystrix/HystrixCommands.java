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

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
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
		private HystrixCommandProperties.Setter commandProperties;
		private boolean eager = false;
		private Function<HystrixObservableCommand<T>, Observable<T>> toObservable;

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

		public PublisherBuilder<T> commandProperties(
				HystrixCommandProperties.Setter commandProperties) {
			this.commandProperties = commandProperties;
			return this;
		}

		public PublisherBuilder<T> commandProperties(
				Function<HystrixCommandProperties.Setter, HystrixCommandProperties.Setter> commandProperties) {
			if (commandProperties == null) {
				throw new IllegalArgumentException(
						"commandProperties must not both be null");
			}
			return this.commandProperties(
					commandProperties.apply(HystrixCommandProperties.Setter()));
		}

		public PublisherBuilder<T> eager() {
			this.eager = true;
			return this;
		}

		public PublisherBuilder<T> toObservable(Function<HystrixObservableCommand<T>, Observable<T>> toObservable) {
			this.toObservable = toObservable;
			return this;
		}

		public Publisher<T> build() {
			if (!StringUtils.hasText(commandName) && setter == null) {
				throw new IllegalStateException("commandName and setter can not both be empty");
			}
			Setter setterToUse = getSetter();

			PublisherHystrixCommand<T> command = new PublisherHystrixCommand<>(setterToUse, this.publisher, this.fallback);

			Observable<T> observable = getObservableFunction().apply(command);

			return RxReactiveStreams.toPublisher(observable);
		}

		public Function<HystrixObservableCommand<T>, Observable<T>> getObservableFunction() {
			Function<HystrixObservableCommand<T>, Observable<T>> observableFunc;

			if (this.toObservable != null) {
				observableFunc = this.toObservable;
			} else if (this.eager) {
				observableFunc = cmd -> cmd.observe();
			} else { // apply a default onBackpressureBuffer if not eager
				observableFunc = cmd -> cmd.toObservable().onBackpressureBuffer();
			}
			return observableFunc;
		}

		public Setter getSetter() {
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
				HystrixCommandProperties.Setter commandProperties = this.commandProperties != null
						? this.commandProperties
						: HystrixCommandProperties.Setter();
				setterToUse = Setter.withGroupKey(groupKey).andCommandKey(commandKey)
						.andCommandPropertiesDefaults(commandProperties);
			}
			return setterToUse;
		}

		public Flux<T> toFlux() {
			return Flux.from(build());
		}

		public Mono<T> toMono() {
			return Mono.from(build());
		}

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
