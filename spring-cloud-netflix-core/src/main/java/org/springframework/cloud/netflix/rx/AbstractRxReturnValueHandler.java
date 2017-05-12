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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import rx.Single;
import rx.functions.Func1;

/**
 * Parent class for rx types handlers.
 * {@link AsyncHandlerMethodReturnValueHandler}
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Kyryl Sablin
 */
abstract public class AbstractRxReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return returnValue != null && supportsReturnType(returnType);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return rxTypeIsAssignableFrom(returnType.getParameterType())
				|| isResponseEntity(returnType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		ResponseEntity<?> responseEntity = getResponseEntity(returnValue);
		if (responseEntity != null) {
			returnValue = responseEntity.getBody();
			if (returnValue == null) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}

		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(
				convertToDeferredResult(responseEntity, returnValue), mavContainer);
	}

	private boolean isResponseEntity(MethodParameter returnType) {
		if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
			Class<?> bodyType = ResolvableType.forMethodParameter(returnType)
					.getGeneric(0).resolve();
			return bodyType != null && rxTypeIsAssignableFrom(bodyType);
		}
		return false;
	}

	protected abstract boolean rxTypeIsAssignableFrom(Class<?> cls);


	protected abstract DeferredResult<?> convertToDeferredResult(ResponseEntity<?> responseEntity, Object returnValue);


	@SuppressWarnings("unchecked")
	private ResponseEntity getResponseEntity(Object returnValue) {
		if (ResponseEntity.class.isAssignableFrom(returnValue.getClass())) {
			return (ResponseEntity) returnValue;

		}
		return null;
	}

	protected DeferredResult<?> convertSingleToDeferredResult(
			final ResponseEntity<?> responseEntity, Single<?> single) {

		// TODO: use lambda when java8 :-)
		Single<ResponseEntity<?>> singleResponse = single
				.map(new Func1<Object, ResponseEntity<?>>() {
					@Override
					public ResponseEntity<?> call(Object object) {
						if (object instanceof ResponseEntity) {
							return (ResponseEntity) object;
						}

						return new ResponseEntity<Object>(object,
								getHttpHeaders(responseEntity),
								getHttpStatus(responseEntity));
					}
				});

		return new SingleDeferredResult<>(singleResponse);
	}

	private HttpStatus getHttpStatus(ResponseEntity<?> responseEntity) {
		if (responseEntity == null) {
			return HttpStatus.OK;
		}
		return responseEntity.getStatusCode();
	}

	private HttpHeaders getHttpHeaders(ResponseEntity<?> responseEntity) {
		if (responseEntity == null) {
			return new HttpHeaders();
		}
		return responseEntity.getHeaders();
	}
}
