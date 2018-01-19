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
 *
 */

package org.springframework.cloud.netflix.feign.support;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import feign.RequestTemplate;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=springencodertest", "spring.jmx.enabled=false" })
@DirtiesContext
public class SpringEncoderTests {

	@Autowired
	private FeignContext context;

	@Autowired
	@Qualifier("myHttpMessageConverter")
	private HttpMessageConverter<?> myConverter;

	@Test
	public void testCustomHttpMessageConverter() {
		SpringEncoder encoder = this.context.getInstance("foo", SpringEncoder.class);
		assertThat(encoder, is(notNullValue()));
		RequestTemplate request = new RequestTemplate();

		encoder.encode("hi", MyType.class, request);

		Collection<String> contentTypeHeader = request.headers().get("Content-Type");
		assertThat("missing content type header", contentTypeHeader, is(notNullValue()));
		assertThat("missing content type header", contentTypeHeader.isEmpty(), is(false));

		String header = contentTypeHeader.iterator().next();
		assertThat("content type header is wrong", header, is("application/mytype"));
		
		assertThat("request charset is null", request.charset(), is(notNullValue()));
		assertThat("request charset is wrong", request.charset(), is(Charset.forName("UTF-8")));
	}

	@Test
	public void testBinaryData() {
		SpringEncoder encoder = this.context.getInstance("foo", SpringEncoder.class);
		assertThat(encoder, is(notNullValue()));
		RequestTemplate request = new RequestTemplate();

		encoder.encode("hi".getBytes(), null, request);

		assertThat("request charset is not null", request.charset(), is(nullValue()));
	}

	@Test
	public void testBinaryHttpMessageConverter() {
		SpringEncoder encoder = this.context.getInstance("foo", SpringEncoder.class);
		assertThat(encoder, is(notNullValue()));
		RequestTemplate request = new RequestTemplate();

		encoder.encode(20, null, request);

		assertThat("request charset is not null", request.charset(), is(nullValue()));
	}
	
	class MediaTypeMatcher implements ArgumentMatcher<MediaType> {

		private MediaType mediaType;

		public MediaTypeMatcher(String type, String subtype) {
			this.mediaType = new MediaType(type, subtype);
		}

		@Override
		public boolean matches(MediaType argument) {
			return this.mediaType.equals(argument);
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("MediaTypeMatcher{");
			sb.append("mediaType=").append(this.mediaType);
			sb.append('}');
			return sb.toString();
		}
	}

	protected static class MyType {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	protected interface TestClient {

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application implements TestClient {

		@Bean
		HttpMessageConverter<?> myHttpMessageConverter() {
			return new MyHttpMessageConverter();
		}

		@Bean
		HttpMessageConverter<?> myBinaryHttpMessageConverter() {
			return new MyBinaryHttpMessageConverter();
		}

		private static class MyBinaryHttpMessageConverter extends AbstractGenericHttpMessageConverter<Integer> implements BinaryHttpMessageConverter<Integer> {

			public MyBinaryHttpMessageConverter() {
				super(new MediaType("application", "mytype"));
			}

			@Override
			public boolean canWrite(Class<?> clazz, MediaType mediaType) {
				return clazz == Integer.class;
			}

			@Override
			protected void writeInternal(Integer i, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

			}

			@Override
			protected Integer readInternal(Class<? extends Integer> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
				return null;
			}

			@Override
			public Integer read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
				return null;
			}
		}

		private static class MyHttpMessageConverter
				extends AbstractGenericHttpMessageConverter<Object> {

			public MyHttpMessageConverter() {
				super(new MediaType("application", "mytype"));
			}

			@Override
			protected boolean supports(Class<?> clazz) {
				return false;
			}

			@Override
			public boolean canRead(Class<?> clazz, MediaType mediaType) {
				return true;
			}

			@Override
			public boolean canWrite(Class<?> clazz, MediaType mediaType) {
				return clazz == String.class;
			}

			@Override
			protected void writeInternal(Object o, Type type,
					HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {

			}

			@Override
			protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
					throws IOException, HttpMessageNotReadableException {
				return null;
			}

			@Override
			public Object read(Type type, Class<?> contextClass,
					HttpInputMessage inputMessage)
					throws IOException, HttpMessageNotReadableException {
				return null;
			}
		}
	}

}
