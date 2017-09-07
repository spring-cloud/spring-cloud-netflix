/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.okhttp;

import static org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer.Runner.customize;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.http.HttpMethod;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonRequest extends ContextAwareRequest implements Cloneable {

	public OkHttpRibbonRequest(RibbonCommandContext context) {
		super(context);
	}

	public Request toRequest() {
		Headers.Builder headers = new Headers.Builder();
		for (String name : this.context.getHeaders().keySet()) {
			List<String> values = this.context.getHeaders().get(name);
			for (String value : values) {
				headers.add(name, value);
			}
		}

		HttpUrl.Builder url = HttpUrl.get(this.uri).newBuilder();
		for (String name : this.context.getParams().keySet()) {
			List<String> values = this.context.getParams().get(name);
			for (String value : values) {
				url.addQueryParameter(name, value);
			}
		}

		RequestBody requestBody = null;

		if (this.context.getRequestEntity() != null && HttpMethod.permitsRequestBody(this.context.getMethod())) {
			MediaType mediaType = null;
			if (headers.get("Content-Type") != null) {
				mediaType = MediaType.parse(headers.get("Content-Type"));
			}
			requestBody = new InputStreamRequestBody(this.context.getRequestEntity(), mediaType, this.context.getContentLength());
		}

		Request.Builder builder = new Request.Builder()
				.url(url.build())
				.headers(headers.build())
				.method(this.context.getMethod(), requestBody);

		customize(this.context.getRequestCustomizers(), builder);

		return builder.build();
	}

	public OkHttpRibbonRequest withNewUri(final URI uri) {
		return new OkHttpRibbonRequest(newContext(uri));
	}

	static class InputStreamRequestBody extends RequestBody {

		private InputStream inputStream;
		private MediaType mediaType;
		private Long contentLength;

		InputStreamRequestBody(InputStream inputStream, MediaType mediaType, Long contentLength) {
			this.inputStream = inputStream;
			this.mediaType = mediaType;
			this.contentLength = contentLength;
		}

		@Override
		public MediaType contentType() {
			return mediaType;
		}

		@Override
		public long contentLength() {
			if (contentLength != null) {
				return contentLength;
			}
			try {
				return inputStream.available();
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			Source source = null;
			try {
				source = Okio.source(inputStream);
				sink.writeAll(source);
			} finally {
				if (source != null) {
					source.close();
				}
			}
		}
	}
}
