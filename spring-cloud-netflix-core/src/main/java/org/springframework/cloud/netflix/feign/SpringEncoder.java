package org.springframework.cloud.netflix.feign;

import com.google.common.base.Charsets;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Created by sgibb on 6/26/14.
 */
public class SpringEncoder extends FeignBase implements Encoder {
	private static final Logger logger = LoggerFactory.getLogger(SpringEncoder.class);

	public SpringEncoder() {
	}

	public SpringEncoder(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	@Override
	public void encode(Object requestBody, RequestTemplate request)
			throws EncodeException {
		// template.body(conversionService.convert(object, String.class));
		if (requestBody != null) {
			Class<?> requestType = requestBody.getClass();
			Collection<String> contentTypes = request.headers().get("Content-Type");

			MediaType requestContentType = null;
			if (contentTypes != null && !contentTypes.isEmpty()) {
				String type = contentTypes.iterator().next();
				requestContentType = MediaType.valueOf(type);
			}

			for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
				if (messageConverter.canWrite(requestType, requestContentType)) {
					if (logger.isDebugEnabled()) {
						if (requestContentType != null) {
							logger.debug("Writing [" + requestBody + "] as \""
									+ requestContentType + "\" using ["
									+ messageConverter + "]");
						}
						else {
							logger.debug("Writing [" + requestBody + "] using ["
									+ messageConverter + "]");
						}

					}

					FeignOutputMessage outputMessage = new FeignOutputMessage(request);
					try {
						@SuppressWarnings("unchecked")
						HttpMessageConverter<Object> copy = (HttpMessageConverter<Object>) messageConverter;
						copy.write(requestBody, requestContentType, outputMessage);
					}
					catch (IOException e) {
						throw new EncodeException("Error converting request body", e);
					}
					request.body(outputMessage.getOutputStream().toByteArray(),
							Charsets.UTF_8); // TODO: set charset
					return;
				}
			}
			String message = "Could not write request: no suitable HttpMessageConverter found for request type ["
					+ requestType.getName() + "]";
			if (requestContentType != null) {
				message += " and content type [" + requestContentType + "]";
			}
			throw new EncodeException(message);
		}
	}

	private class FeignOutputMessage implements HttpOutputMessage {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		RequestTemplate request;

		private FeignOutputMessage(RequestTemplate request) {
			this.request = request;
		}

		@Override
		public OutputStream getBody() throws IOException {
			return outputStream;
		}

		@Override
		public HttpHeaders getHeaders() {
			return getHttpHeaders(request.headers());
		}

		public ByteArrayOutputStream getOutputStream() {
			return outputStream;
		}
	}
}
