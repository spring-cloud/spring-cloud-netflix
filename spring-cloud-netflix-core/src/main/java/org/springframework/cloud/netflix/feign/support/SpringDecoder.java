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

package org.springframework.cloud.netflix.feign.support;

import static org.springframework.cloud.netflix.feign.support.FeignUtils.getHttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

/**
 * @author Spencer Gibb
 */
public class SpringDecoder implements Decoder {

	private ObjectFactory<HttpMessageConverters> messageConverters;

	public SpringDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		this.messageConverters = messageConverters;
	}

	@Override
	public Object decode(final Response response, Type type)
			throws IOException, FeignException {
		if (type instanceof Class || type instanceof ParameterizedType
				|| type instanceof WildcardType) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(
					type, this.messageConverters.getObject().getConverters());

			return extractor.extractData(new FeignResponseAdapter(response));
		}
		throw new DecodeException(
				"type is not an instance of Class or ParameterizedType: " + type);
	}

	private class FeignResponseAdapter implements ClientHttpResponse {

		private final Response response;

		private FeignResponseAdapter(Response response) {
			this.response = response;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return HttpStatus.valueOf(this.response.status());
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return this.response.status();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.response.reason();
		}

		@Override
		public void close() {
			try {
				this.response.body().close();
			}
			catch (IOException ex) {
				// Ignore exception on close...
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.response.body().asInputStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			return getHttpHeaders(this.response.headers());
		}

	}

}
