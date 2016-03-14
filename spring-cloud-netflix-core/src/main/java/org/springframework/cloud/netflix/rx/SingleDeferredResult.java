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

import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;

import rx.Single;

/**
 * A specialized {@link DeferredResult} that handles {@link Single} return type.
 *
 * @author Jakub Narloch
 * @see DeferredResult
 */
class SingleDeferredResult<T> extends DeferredResult<T> {

    private static final Object EMPTY_RESULT = new Object();

    private final DeferredResultSubscriber<T> subscriber;

    public SingleDeferredResult(Single<T> single) {
        this(null, EMPTY_RESULT, single);
    }

    public SingleDeferredResult(long timeout, Single<T> single) {
        this(timeout, EMPTY_RESULT, single);
    }

    public SingleDeferredResult(Long timeout, Object timeoutResult, Single<T> single) {
        super(timeout, timeoutResult);
        Assert.notNull(single, "single can not be null");

        subscriber = new DeferredResultSubscriber<>(single.toObservable(), this);
    }
}
