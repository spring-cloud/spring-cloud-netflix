/*
 * Copyright 2013-2017 the original author or authors.
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
 */

package org.springframework.cloud.netflix.rx;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import rx.Observable;

/**
 * A convenient class allowing to wrap either the {@link Observable} into a response supported by the
 * Spring MVC.
 *
 * @author Jakub Narloch
 */
public final class RxResponse {

	private RxResponse() {

	}

	/**
	 * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
	 * as server side event.
	 *
	 * @param observable the observable instance
	 * @param <T> the result type
	 * @return the sse emitter
	 */
	public static <T> SseEmitter sse(Observable<T> observable) {
		return new ObservableSseEmitter<>(observable);
	}

	/**
	 * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
	 * as server side event.
	 *
	 * @param mediaType the media type of produced entry
	 * @param observable the observable instance
	 * @param <T> the result type
	 * @return the sse emitter
	 */
	public static <T> SseEmitter sse(MediaType mediaType, Observable<T> observable) {
		return new ObservableSseEmitter<>(mediaType, observable);
	}

	/**
	 * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
	 * as server side event.
	 *
	 * @param timeout the response timeout
	 * @param mediaType the media type of produced entry
	 * @param observable the observable instance
	 * @param <T> the result type
	 * @return the sse emitter
	 */
	public static <T> SseEmitter sse(long timeout, MediaType mediaType, Observable<T> observable) {
		return new ObservableSseEmitter<>(timeout, mediaType, observable);
	}
}
