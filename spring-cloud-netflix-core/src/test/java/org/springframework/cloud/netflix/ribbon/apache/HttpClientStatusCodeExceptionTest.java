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
package org.springframework.cloud.netflix.ribbon.apache;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Locale;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpClientStatusCodeExceptionTest {

	@Test
	public void getResponse() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		doReturn(new Locale("en")).when(response).getLocale();
		Header foo = new BasicHeader("foo", "bar");
		Header[] headers = new Header[]{foo};
		doReturn(headers).when(response).getAllHeaders();
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("http", 1, 1),
				200, "Success");
		doReturn(statusLine).when(response).getStatusLine();
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(new ByteArrayInputStream("foo".getBytes()));
		entity.setContentLength(3);
		doReturn(entity).when(response).getEntity();
		HttpEntity copiedEntity = HttpClientUtils.createEntity(response);
		HttpClientStatusCodeException ex = new HttpClientStatusCodeException("service", response, copiedEntity,
				new URI("http://service.com"));
		assertEquals("en", ex.getResponse().getLocale().toString());
		assertArrayEquals(headers, ex.getResponse().getAllHeaders());
		assertEquals("Success", ex.getResponse().getStatusLine().getReasonPhrase());
		assertEquals(200, ex.getResponse().getStatusLine().getStatusCode());
		assertEquals("http", ex.getResponse().getStatusLine().getProtocolVersion().getProtocol());
		assertEquals(1, ex.getResponse().getStatusLine().getProtocolVersion().getMajor());
		assertEquals(1, ex.getResponse().getStatusLine().getProtocolVersion().getMinor());
		assertEquals("foo", EntityUtils.toString(ex.getResponse().getEntity()));
		verify(response, times(1)).close();
	}
}