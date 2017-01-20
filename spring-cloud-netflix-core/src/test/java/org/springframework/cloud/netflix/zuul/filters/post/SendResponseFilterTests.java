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

package org.springframework.cloud.netflix.zuul.filters.post;

import java.io.ByteArrayInputStream;
import javax.servlet.http.HttpServletRequest;

import com.netflix.config.ConfigurationManager;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
public class SendResponseFilterTests {

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void reset() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void runsNormally() throws Exception {
		String characterEncoding = null;
		String content = "hello";
		runFilter(characterEncoding, content, false);
	}

	@Test
	public void characterEncodingNotOverridden() throws Exception {
		String characterEncoding = "UTF-16";
		String content = "\u00a5";
		runFilter(characterEncoding, content, true);
	}

	@Test
	public void runWithDebugHeader() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(ZuulConstants.ZUUL_INCLUDE_DEBUG_HEADER, true);

		SendResponseFilter filter = createFilter("hello", null, new MockHttpServletResponse(), false);
		Debug.addRoutingDebug("test");
		filter.run();

		String debugHeader = RequestContext.getCurrentContext().getResponse()
				.getHeader("X-Zuul-Debug-Header");
		assertThat("wrong debug header", debugHeader, equalTo("[[[test]]]"));
	}

	@Test
	public void runWithOriginContentLength() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(ZuulConstants.ZUUL_SET_CONTENT_LENGTH, true);

		SendResponseFilter filter = createFilter("hello", null, new MockHttpServletResponse(), false);
		RequestContext.getCurrentContext().setOriginContentLength(6L); // for test
		RequestContext.getCurrentContext().setResponseGZipped(false);
		filter.run();

		String contentLength = RequestContext.getCurrentContext().getResponse()
				.getHeader("Content-Length");
		assertThat("wrong origin content length", contentLength, equalTo("6"));
	}

	private void runFilter(String characterEncoding, String content, boolean streamContent) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		SendResponseFilter filter = createFilter(content, characterEncoding, response, streamContent);
		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
		String encoding = RequestContext.getCurrentContext().getResponse().getCharacterEncoding();
		String expectedEncoding = characterEncoding != null ? characterEncoding : WebUtils.DEFAULT_CHARACTER_ENCODING;
		assertThat("wrong character encoding", encoding, equalTo(expectedEncoding));
		assertThat("wrong content", response.getContentAsString(), equalTo(content));
	}

	private SendResponseFilter createFilter(String content, String characterEncoding, MockHttpServletResponse response, boolean streamContent) throws Exception {
		HttpServletRequest request = new MockHttpServletRequest();
		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.setResponse(response);

		if (characterEncoding != null) {
			response.setCharacterEncoding(characterEncoding);
		}

		if (streamContent) {
			context.setResponseDataStream(new ByteArrayInputStream(content.getBytes(characterEncoding)));
		} else {
			context.setResponseBody(content);
		}

		context.addZuulResponseHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length()));

		context.set("error.status_code", HttpStatus.NOT_FOUND.value());
		RequestContext.testSetCurrentContext(context);
		SendResponseFilter filter = new SendResponseFilter();
		return filter;
	}

}