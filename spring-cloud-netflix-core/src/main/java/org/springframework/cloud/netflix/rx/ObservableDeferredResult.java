/*
 * Copyright 2013-2015 the original author or authors.
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
import rx.functions.Action1;

/**
 * Convert an {@link Observable} to a {@link DeferredResult}
 *
 * Inspired by http://www.nurkiewicz.com/2013/03/deferredresult-asynchronous-processing.html
 * @author Spencer Gibb
 */
public class ObservableDeferredResult<T> extends DeferredResult<T> {
	public ObservableDeferredResult(Observable<T> observable) {
		observable.subscribe(new Action1<T>() {
			@Override
			public void call(T t) {
				setResult(t);
			}
		}, new Action1<Throwable>() {
			@Override
			public void call(Throwable throwable) {
				setErrorResult(throwable);
			}
		});
	}
}
