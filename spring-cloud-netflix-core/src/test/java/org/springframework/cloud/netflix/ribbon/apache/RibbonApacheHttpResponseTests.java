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

package org.springframework.cloud.netflix.ribbon.apache;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * @author Spencer Gibb
 */
public class RibbonApacheHttpResponseTests {

	@Test
	public void testNullEntity() throws Exception {
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(204);
		HttpResponse response = mock(HttpResponse.class);
		given(response.getStatusLine()).willReturn(statusLine);

		RibbonApacheHttpResponse httpResponse = new RibbonApacheHttpResponse(response, URI.create("http://example.com"));

		assertThat(httpResponse.isSuccess(), is(true));
		assertThat(httpResponse.hasPayload(), is(false));
		assertThat(httpResponse.getPayload(), is(nullValue()));
		assertThat(httpResponse.getInputStream(), is(nullValue()));
	}


	@Test
	public void testNotNullEntity() throws Exception {
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(204);
		HttpResponse response = mock(HttpResponse.class);
		given(response.getStatusLine()).willReturn(statusLine);
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(new ByteArrayInputStream(new byte[0]));
		given(response.getEntity()).willReturn(entity);

		RibbonApacheHttpResponse httpResponse = new RibbonApacheHttpResponse(response, URI.create("http://example.com"));

		assertThat(httpResponse.isSuccess(), is(true));
		assertThat(httpResponse.hasPayload(), is(true));
		assertThat(httpResponse.getPayload(), is(notNullValue()));
		assertThat(httpResponse.getInputStream(), is(notNullValue()));
	}
}
