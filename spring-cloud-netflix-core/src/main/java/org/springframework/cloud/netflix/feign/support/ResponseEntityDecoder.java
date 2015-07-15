package org.springframework.cloud.netflix.feign.support;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;

/**
 * Decoder adds compatibility for Spring MVC's ResponseEntity to any other decoder via
 * composition.
 * @author chadjaros
 */
@Slf4j
public class ResponseEntityDecoder implements Decoder {

	private Decoder decoder;

	public ResponseEntityDecoder(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public Object decode(final Response response, Type type) throws IOException,
			FeignException {

		if (type instanceof ParameterizedType
				&& ((ParameterizedType) type).getRawType().equals(ResponseEntity.class)) {

			type = ((ParameterizedType) type).getActualTypeArguments()[0];
			Object decodedObject = decoder.decode(response, type);

			return createResponse(decodedObject, response);
		}
		else {
			return decoder.decode(response, type);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> ResponseEntity<T> createResponse(Object instance, Response response) {

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		for (String key : response.headers().keySet()) {
			headers.put(key, new LinkedList<>(response.headers().get(key)));
		}

		return new ResponseEntity<>((T) instance, headers, HttpStatus.valueOf(response
				.status()));
	}
}