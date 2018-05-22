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
 *
 */

package org.springframework.cloud.netflix.zuul.util;

import com.netflix.zuul.exception.ZuulException;
import org.springframework.http.HttpStatus;

/**
 * @author Spencer Gibb
 */
public class ZuulRuntimeException extends RuntimeException {

	public ZuulRuntimeException(ZuulException cause) {
		super(cause);
	}

	public ZuulRuntimeException(Exception ex) {
		this(new ZuulException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
	}
}
