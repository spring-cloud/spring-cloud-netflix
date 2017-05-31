/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.ribbon.support;

import java.io.IOException;

/**
 * Exception to be thrown when the status code is deemed to be retryable.
 * @author Ryan Baxter
 * @deprecated Use {@link org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException} instead
 */
//TODO Remove in Edgeware
@Deprecated
public class RetryableStatusCodeException extends IOException {

	private static final String MESSAGE = "Service %s returned a status code of %d";

	public RetryableStatusCodeException(String serviceId, int statusCode) {
		super(String.format(MESSAGE, serviceId, statusCode));
	}
}
