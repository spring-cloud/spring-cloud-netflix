package org.springframework.platform.netflix.feign;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by sgibb on 6/26/14.
 */
public class SpringDecoder extends FeignBase implements Decoder {
    private static final Logger logger = LoggerFactory.getLogger(SpringDecoder.class);

    public SpringDecoder() {
    }

    public SpringDecoder(List<HttpMessageConverter<?>> messageConverters) {
        super(messageConverters);
    }

    @Override
    public Object decode(final Response response, Type type) throws IOException, DecodeException, FeignException {
        if (type instanceof Class) {
            HttpMessageConverterExtractor<?> extractor =
                    new HttpMessageConverterExtractor((Class<?>) type, getMessageConverters());

            Object data = extractor.extractData(new FeignResponseAdapter(response));
            return data;
        }
        throw new DecodeException("type is not an instance of Class: "+type);
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
            } catch (IOException e) {
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
