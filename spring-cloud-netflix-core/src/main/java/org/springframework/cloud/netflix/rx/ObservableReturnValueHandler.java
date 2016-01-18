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
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles return values of type {@link rx.Observable}.
 *
 * @author Spencer Gibb
 */
public class ObservableReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return returnValue != null && returnValue instanceof Observable;
	}

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
		final ValuesCollector collector = new ValuesCollector();
		final DeferredResult<Object> deferredResult = new DeferredResult<>();

        observable.subscribe(new Action1<Object>() {
			@Override
			public void call(Object o) {
				collector.collect(o);
			}
		}, new Action1<Throwable>() {
			@Override
			public void call(Throwable throwable) {
				deferredResult.setErrorResult(throwable);
			}
		}, new Action0() {
			@Override
			public void call() {
				deferredResult.setResult(collector.getValue());
			}
		});

		WebAsyncUtils.getAsyncManager(webRequest)
				.startDeferredResultProcessing(deferredResult, mavContainer);
	}

	/**
	 * Accumulates the values produced into a collection. Depending on whether a single value
	 * has been collected or multiple, the values are going to be wrapped into an array when
	 * {@link #getValue()} will be called. Otherwise the single collected value will be returned.
	 */
	@SuppressWarnings("unchecked")
	private static class ValuesCollector {

		private final List values = new ArrayList<>();

		public void collect(Object value) {
			values.add(value);
		}

		public Object getValue() {
			if(isSingle()) {
				return getFirst();
			} else {
				return getArray();
			}
		}

		private boolean isSingle() {
			return values.size() == 1;
		}

		private Object getFirst() {
			return values.get(0);
		}

		private Object[] getArray() {
			return values.toArray(new Object[values.size()]);
		}
	}
}
