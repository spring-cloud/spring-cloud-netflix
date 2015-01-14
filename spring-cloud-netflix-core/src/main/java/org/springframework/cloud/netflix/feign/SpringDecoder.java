package org.springframework.cloud.netflix.feign;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import static org.springframework.cloud.netflix.feign.FeignUtils.getHttpHeaders;

/**
 * @author Spencer Gibb
 */
public class SpringDecoder implements Decoder {

	@Autowired
	Provider<HttpMessageConverters> messageConverters;

	public SpringDecoder() {
	}

	@Override
	public Object decode(final Response response, Type type) throws IOException,
			FeignException {
		if (type instanceof Class || type instanceof ParameterizedType) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(
					type, this.messageConverters.get().getConverters());

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
			catch (IOException e) {
				e.printStackTrace();
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
