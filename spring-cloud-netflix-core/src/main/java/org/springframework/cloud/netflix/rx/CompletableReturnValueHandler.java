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

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;

import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * A specialized {@link AbstractRxReturnValueHandler} that handles {@link rx.Completable}
 * return type.
 *
 * @author Kyryl Sablin
 */
public class CompletableReturnValueHandler extends AbstractRxReturnValueHandler {

	@Override
	protected boolean rxTypeIsAssignableFrom(Class<?> cls) {
		return Completable.class.isAssignableFrom(cls);
	}

	@Override
	protected void startDeferredResultProcessing(Object returnValue, ModelAndViewContainer mavContainer,
												 NativeWebRequest webRequest, ResponseEntity<?> responseEntity)
			throws Exception {
		final Completable completable = Completable.class.cast(returnValue);
		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(
				convertToDeferredResult(responseEntity, fromCompletable(completable)), mavContainer);
	}

	private Single<?> fromCompletable(Completable completable) {
		return completable.toObservable().concatWith(Observable.just(null)).toSingle();
	}
}
