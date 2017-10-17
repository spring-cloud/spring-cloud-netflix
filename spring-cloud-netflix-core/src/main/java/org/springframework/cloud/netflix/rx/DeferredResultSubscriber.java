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

import org.springframework.web.context.request.async.DeferredResult;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * A subscriber that sets the single value produced by the {@link Observable} on the {@link DeferredResult}.
 *
 * @author Jakub Narloch
 * @see DeferredResult
 */
class DeferredResultSubscriber<T> extends Subscriber<T> implements Runnable {

	private final DeferredResult<T> deferredResult;

	private final Subscription subscription;

	private boolean completed;

	public DeferredResultSubscriber(Observable<T> observable, DeferredResult<T> deferredResult) {

		this.deferredResult = deferredResult;
		this.deferredResult.onTimeout(this);
		this.deferredResult.onCompletion(this);
		this.subscription = observable.subscribe(this);
	}

	@Override
	public void onNext(T value) {
		if (!completed) {
			deferredResult.setResult(value);
		}
	}

	@Override
	public void onError(Throwable e) {
		deferredResult.setErrorResult(e);
	}

	@Override
	public void onCompleted() {
		completed = true;
	}

	@Override
	public void run() {
		this.subscription.unsubscribe();
	}
}
