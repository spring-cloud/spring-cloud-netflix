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

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import rx.Observable;

/**
 * A specialized {@link SseEmitter} that handles {@link Observable} return types.
 *
 * @author Jakub Narloch
 * @see SseEmitter
 */
class ObservableSseEmitter<T> extends SseEmitter {

	public ObservableSseEmitter(Observable<T> observable) {
		this(null, observable);
	}

	public ObservableSseEmitter(MediaType mediaType, Observable<T> observable) {
		this(null, mediaType, observable);
	}

	public ObservableSseEmitter(Long timeout, MediaType mediaType,
			Observable<T> observable) {
		super(timeout);
		new ResponseBodyEmitterSubscriber<>(mediaType, observable, this);
	}
}
