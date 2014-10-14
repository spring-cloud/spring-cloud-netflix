package org.springframework.cloud.netflix.feign;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

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
    HttpMessageConverters messageConverters;

	public SpringDecoder() {
	}

	@Override
	public Object decode(final Response response, Type type) throws IOException, FeignException {
		if (type instanceof Class) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(
					(Class<?>) type, messageConverters.getConverters());

			Object data = extractor.extractData(new FeignResponseAdapter(response));
			return data;
		}
		throw new DecodeException("type is not an instance of Class: " + type);
	}

	private class FeignResponseAdapter implements ClientHttpResponse {
		private final Response response;

		private FeignResponseAdapter(Response response) {
			this.response = response;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return HttpStatus.valueOf(response.status());
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.status();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.reason();
		}

		@Override
		public void close() {
			try {
				response.body().close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return response.body().asInputStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			return getHttpHeaders(response.headers());
		}

	}
}
