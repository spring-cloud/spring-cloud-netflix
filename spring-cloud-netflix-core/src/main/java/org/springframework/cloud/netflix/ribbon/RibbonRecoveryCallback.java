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
package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;

import java.net.URI;

/**
 * @author LiYuan Lee
 */
public abstract class RibbonRecoveryCallback<T, R> implements RecoveryCallback<T> {

	protected abstract T createResponse(R response, URI uri);

	@Override
	public T recover(RetryContext context) throws Exception {
		Throwable lastThrowable = context.getLastThrowable();
		if (lastThrowable != null && lastThrowable instanceof RetryableStatusCodeException) {
			RetryableStatusCodeException ex = (RetryableStatusCodeException) lastThrowable;
			return createResponse((R) ex.getResponse(), ex.getUri());
		}
		throw new RetryException("Could not recover", lastThrowable);
	}
}
