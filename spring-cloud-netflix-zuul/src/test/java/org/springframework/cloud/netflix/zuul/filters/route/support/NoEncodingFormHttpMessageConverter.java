/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route.support;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * @author Jacques-Etienne Beaudet
 */
public class NoEncodingFormHttpMessageConverter extends FormHttpMessageConverter {

	@SuppressWarnings("unchecked")
	@Override
	public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		MultiValueMap<String, String> form = (MultiValueMap<String, String>) map;
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
			String name = nameIterator.next();
			for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext();) {
				String value = valueIterator.next();
				builder.append(name);
				if (value != null) {
					builder.append('=');
					builder.append(value);
					if (valueIterator.hasNext()) {
						builder.append('&');
					}
				}
			}
			if (nameIterator.hasNext()) {
				builder.append('&');
			}
		}
		final byte[] bytes = builder.toString().getBytes(FormHttpMessageConverter.DEFAULT_CHARSET);
		outputMessage.getHeaders().setContentLength(bytes.length);
		outputMessage.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		StreamUtils.copy(bytes, outputMessage.getBody());
	}
}
