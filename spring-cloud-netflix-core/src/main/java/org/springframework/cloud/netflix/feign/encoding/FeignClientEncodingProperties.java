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

package org.springframework.cloud.netflix.feign.encoding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Objects;

/**
 * The Feign encoding properties.
 *
 * @author Jakub Narloch
 */
@ConfigurationProperties("feign.compression.request")
public class FeignClientEncodingProperties {

	/**
	 * The list of supported mime types.
	 */
	private String[] mimeTypes = new String[]{"text/xml", "application/xml", "application/json"};

	/**
	 * The minimum threshold content size.
	 */
	private int minRequestSize = 2048;

	public String[] getMimeTypes() {
		return mimeTypes;
	}

	public void setMimeTypes(String[] mimeTypes) {
		this.mimeTypes = mimeTypes;
	}

	public int getMinRequestSize() {
		return minRequestSize;
	}

	public void setMinRequestSize(int minRequestSize) {
		this.minRequestSize = minRequestSize;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FeignClientEncodingProperties that = (FeignClientEncodingProperties) o;
		return Arrays.equals(mimeTypes, that.mimeTypes) &&
				Objects.equals(minRequestSize, that.minRequestSize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mimeTypes, minRequestSize);
	}

	@Override
	public String toString() {
		return new StringBuilder("FeignClientEncodingProperties{")
				.append("mimeTypes=").append(Arrays.toString(mimeTypes)).append(", ")
				.append("minRequestSize=").append(minRequestSize)
				.append("}").toString();
	}

}
