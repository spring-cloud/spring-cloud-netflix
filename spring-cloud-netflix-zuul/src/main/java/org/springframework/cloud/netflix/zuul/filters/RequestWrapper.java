/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.Objects;

import org.springframework.http.HttpMethod;

/**
 * Holder class for data which might be needed for routing purposes.
 *
 *
 * @author Arnold Galovics
 */
public class RequestWrapper {
	private final String path;
	private final HttpMethod method;

	private RequestWrapper(String path, HttpMethod method) {
		this.path = path;
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		RequestWrapper that = (RequestWrapper) o;

		if (path != null ? !path.equals(that.path) : that.path != null)
			return false;
		return method == that.method;
	}

	@Override
	public int hashCode() {
		int result = path != null ? path.hashCode() : 0;
		result = 31 * result + (method != null ? method.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RequestWrapper{" + "path='" + path + '\'' + ", method=" + method + '}';
	}

	public static RequestWrapper from(String path, HttpMethod method) {
		Objects.requireNonNull(path, "path must not be null");
		Objects.requireNonNull(method, "method must not be null");
		return new RequestWrapper(path, method);
	}
}
