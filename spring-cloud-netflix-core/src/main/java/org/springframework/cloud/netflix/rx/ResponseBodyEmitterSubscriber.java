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

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;


/**
 * Subscriber that emits any value produced by the {@link Observable} into the delegated {@link ResponseBodyEmitter}.
 *
 * @author Jakub Narloch
 */
class ResponseBodyEmitterSubscriber<T> extends Subscriber<T> implements Runnable {

	private final MediaType mediaType;

	private final Subscription subscription;

	private final ResponseBodyEmitter responseBodyEmitter;

	private boolean completed;

	/**
	 * Creates new instance of {@link ResponseBodyEmitterSubscriber} with response media type, observable and response
	 * emitter.
	 *
	 * @param mediaType the marshaled object media type
	 * @param observable the observable
	 * @param responseBodyEmitter the response emitter
	 */
	public ResponseBodyEmitterSubscriber(MediaType mediaType, Observable<T> observable, ResponseBodyEmitter responseBodyEmitter) {

		this.mediaType = mediaType;
		this.responseBodyEmitter = responseBodyEmitter;
		this.responseBodyEmitter.onTimeout(this);
		this.responseBodyEmitter.onCompletion(this);
		this.subscription = observable.subscribe(this);
	}

	@Override
	public void onNext(T value) {

		try {
			if(!completed) {
				responseBodyEmitter.send(value, mediaType);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void onError(Throwable e) {
		responseBodyEmitter.completeWithError(e);
	}

	@Override
	public void onCompleted() {
		if(!completed) {
			completed = true;
			responseBodyEmitter.complete();
		}
	}

	@Override
	public void run() {
		subscription.unsubscribe();
	}
}
