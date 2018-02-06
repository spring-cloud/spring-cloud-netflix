/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.cloud.netflix.feign.ribbon;

import feign.Request;
import feign.Response;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class RibbonResponseStatusCodeExceptionTest {

	@Test
	public void getResponse() throws Exception {
		Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
		List<String> fooValues = new ArrayList<String>();
		fooValues.add("bar");
		headers.put("foo", fooValues);
		Request request = Request.create("GET", "http://service.com",
				new HashMap<String, Collection<String>>(), new byte[]{}, Charset.defaultCharset());
		byte[] body = "foo".getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(body);
		Response response = Response.builder().status(200).reason("Success").request(request).body(is, body.length).headers(headers).build();
		RibbonResponseStatusCodeException ex = new RibbonResponseStatusCodeException("service", response, body,
				new URI(request.url()));
		assertEquals(200, ex.getResponse().status());
		assertEquals(request, ex.getResponse().request());
		assertEquals("Success", ex.getResponse().reason());
		assertEquals("foo", StreamUtils.copyToString(ex.getResponse().body().asInputStream(), Charset.defaultCharset()));
	}
}