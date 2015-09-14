
package org.springframework.cloud.netflix.feign.support;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * Decoder adds compatibility for Spring MVC's ResponseEntity to any
 * other decoder via composition.
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

        if(type instanceof ParameterizedType &&
                ((ParameterizedType) type).getRawType().equals(ResponseEntity.class)) {

            type = ((ParameterizedType) type).getActualTypeArguments()[0];
            Object decodedObject = decoder.decode(response, type);

            Class<?> clazz = null;
            if (decodedObject != null) {
                clazz = decodedObject.getClass();
            }
            return createResponse(
                    clazz,
                    decodedObject,
                    response);
        }
        else {
            return decoder.decode(response, type);
        }
    }

    private <T> ResponseEntity<T> createResponse(Class<T> clazz, Object instance, Response response) {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        for(String key: response.headers().keySet()) {
            headers.put(key, new LinkedList<>(response.headers().get(key)));
        }

        T retVal = null;
        if (clazz != null && instance != null) {
            retVal = clazz.cast(instance);
        }
        return new ResponseEntity<>(retVal, headers,
                HttpStatus.valueOf(response.status()));
    }
}