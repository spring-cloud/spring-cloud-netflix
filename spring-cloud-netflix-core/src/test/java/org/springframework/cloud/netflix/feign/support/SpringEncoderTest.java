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

import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SpringEncoderTest {

    SpringEncoder springEncoder;
    HttpHeaders newHeaders;

    @Before
    public void setUp() throws Exception {
        newHeaders = new HttpHeaders();

        springEncoder = new SpringEncoder(new ObjectFactory<HttpMessageConverters>() {
            @Override
            public HttpMessageConverters getObject() throws BeansException {
                return new HttpMessageConverters(new HeaderModifierMessageConverter(newHeaders));
            }
        });
    }

    @Test
    public void testEncode_addedHeaders() throws Exception {
        newHeaders.set("Custom-Header", "custom value");

        RequestTemplate request = new RequestTemplate();
        springEncoder.encode("body", String.class, request);

        Collection<String> values = request.headers().get("Custom-Header");
        assertThat(values.iterator().next(), is("custom value"));
    }

    @Test
    public void testEncode_existingHeaders() throws Exception {
        newHeaders.set("Added-Header", "added");

        RequestTemplate request = new RequestTemplate().header("Existing-Header", "existing");
        springEncoder.encode("body", String.class, request);

        Collection<String> values;
        values = request.headers().get("Added-Header");
        assertThat(values.iterator().next(), is("added"));

        values = request.headers().get("Existing-Header");
        assertThat(values.iterator().next(), is("existing"));
    }

    @Test
    public void testEncode_replacedHeaders() throws Exception {
        newHeaders.set("Some-Header", "changed");

        RequestTemplate request = new RequestTemplate().header("Some-Header", "original");
        springEncoder.encode("body", String.class, request);

        Collection<String> values = request.headers().get("Some-Header");
        assertThat(values.iterator().next(), is("changed"));
    }

    @RequiredArgsConstructor
    static class HeaderModifierMessageConverter implements HttpMessageConverter<Object> {

        private final HttpHeaders headers;

        @Override
        public boolean canWrite(Class clazz, MediaType mediaType) {
            return true;
        }

        @Override
        public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
            outputMessage.getHeaders().putAll(headers);
        }

        @Override
        public boolean canRead(Class clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
            return null;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return null;
        }

    }

}