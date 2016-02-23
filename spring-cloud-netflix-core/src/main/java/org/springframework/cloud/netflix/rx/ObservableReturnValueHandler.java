/*
 * Copyright 2013-2016 the original author or authors.
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
import rx.Observable;
import rx.functions.Func1;

import java.util.List;

/**
 * A specialized {@link AsyncHandlerMethodReturnValueHandler} that handles {@link Observable} return types.
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
public class ObservableReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

    @Override
    public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
        return returnValue != null && supportsReturnType(returnType);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return Observable.class.isAssignableFrom(returnType.getParameterType()) || isResponseEntity(returnType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }

        ResponseEntity<?> responseEntity = getResponseEntity(returnValue);
        if(responseEntity != null) {
            returnValue = responseEntity.getBody();
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                return;
            }
        }

        final Observable<?> observable = Observable.class.cast(returnValue);
        WebAsyncUtils.getAsyncManager(webRequest)
                .startDeferredResultProcessing(convertToDeferredResult(responseEntity, observable), mavContainer);
    }

    protected DeferredResult<?> convertToDeferredResult(final ResponseEntity<?> originalResponseEntity, Observable<?> observable) {

        Observable<ResponseEntity<List<?>>> observableResponse =
                observable.toList().map(new ResponseEntityAdapter(originalResponseEntity));
        return new SingleDeferredResult<>(observableResponse.toSingle());
    }

    private boolean isResponseEntity(MethodParameter returnType) {
        if(ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
            return bodyType != null && Observable.class.isAssignableFrom(bodyType);
        }
        return false;
    }

    private ResponseEntity<?> getResponseEntity(Object returnValue) {
        if (ResponseEntity.class.isAssignableFrom(returnValue.getClass())) {
            return (ResponseEntity<?>) returnValue;

        }
        return null;
    }

    private static final class ResponseEntityAdapter implements Func1<List<?>, ResponseEntity<List<?>>> {

        private final ResponseEntity<?> originalResponseEntity;

        ResponseEntityAdapter(ResponseEntity<?> originalResponseEntity) {
            this.originalResponseEntity = originalResponseEntity;
        }

        @Override
        public ResponseEntity<List<?>> call(List<?> list) {
            return new ResponseEntity<List<?>>(list, getHttpHeaders(), getHttpStatus());
        }

        HttpStatus getHttpStatus() {
            if(originalResponseEntity == null) {
                return HttpStatus.OK;
            }
            return originalResponseEntity.getStatusCode();
        }

        HttpHeaders getHttpHeaders() {
            if(originalResponseEntity == null) {
                return new HttpHeaders();
            }
            return originalResponseEntity.getHeaders();
        }
    }
}
