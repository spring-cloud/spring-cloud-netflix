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

import feign.RequestTemplate;

import java.util.Collection;
import java.util.Map;

/**
 * Enables the HTTP request payload compression by specifying the {@code Content-Encoding} headers.
 *
 * @author Jakub Narloch
 */
public class FeignContentGzipEncodingInterceptor extends BaseRequestInterceptor {

	/**
	 * Creates new instance of {@link FeignContentGzipEncodingInterceptor}.
	 *
	 * @param properties the encoding properties
	 */
	protected FeignContentGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
		super(properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void apply(RequestTemplate template) {

		if (requiresCompression(template)) {
			addHeader(template, HttpEncoding.CONTENT_ENCODING_HEADER, HttpEncoding.GZIP_ENCODING,
					HttpEncoding.DEFLATE_ENCODING);
		}
	}

	/**
	 * Returns whether the request requires GZIP compression.
	 *
	 * @param template the request template
	 * @return true if request requires compression, false otherwise
	 */
	private boolean requiresCompression(RequestTemplate template) {

		final Map<String, Collection<String>> headers = template.headers();
		return matchesMimeType(headers.get(HttpEncoding.CONTENT_TYPE))
				&& contentLengthExceedThreshold(headers.get(HttpEncoding.CONTENT_LENGTH));
	}

	/**
	 * Returns whether the request content length exceed configured minimum size.
	 *
	 * @param contentLength the content length header value
	 * @return true if length is grater than minimum size, false otherwise
	 */
	private boolean contentLengthExceedThreshold(Collection<String> contentLength) {

		try {
			if (contentLength == null || contentLength.size() != 1) {
				return false;
			}

			final String strLen = contentLength.iterator().next();
			final long length = Long.parseLong(strLen);
			return length > getProperties().getMinRequestSize();
		} catch (NumberFormatException ex) {
			// ignores the exception
		}
		return false;
	}

	/**
	 * Returns whether the content mime types matches the configures mime types.
	 *
	 * @param contentTypes the content types
	 * @return true if any specified content type matches the request content types
	 */
	private boolean matchesMimeType(Collection<String> contentTypes) {
		if (contentTypes == null || contentTypes.size() == 0) {
			return false;
		}

		if (getProperties().getMimeTypes() == null || getProperties().getMimeTypes().length == 0) {
			// no specific mime types has been set - matching everything
			return true;
		}

		for (String mimeType : getProperties().getMimeTypes()) {
			if (contentTypes.contains(mimeType)) {
				return true;
			}
		}

		return false;
	}
}
