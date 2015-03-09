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

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import rx.Observable;
import rx.functions.Action1;

/**
 * Handles return values of type {@link rx.Observable}.
 *
 * @author Spencer Gibb
 */
public class ObservableReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return Observable.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		Observable<?> observable = Observable.class.cast(returnValue);

		final DeferredResult<Object> deferredResult = new DeferredResult<>();

        observable.subscribe(new Action1<Object>() {
            @Override
            public void call(Object o) {
                deferredResult.setResult(o);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                deferredResult.setErrorResult(throwable);
            }
        });

		WebAsyncUtils.getAsyncManager(webRequest)
				.startDeferredResultProcessing(deferredResult, mavContainer);
	}
}
