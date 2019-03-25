/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockMultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Hartle
 */
public class FormBodyWrapperFilterTests {

	private FormBodyWrapperFilter filter;

	private MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();

	@Before
	public void init() {
		this.filter = new FormBodyWrapperFilter();
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
	}

	@Test
	public void multiplePartNamesWithMultipleParts()
			throws IOException, ServletException {
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");

		final Map<String, List<String>> firstPartHeaders = new HashMap<>();
		final byte[] firstPartBody = "{ \"u\" : 1 }".getBytes();
		final Part firstPart = new MockPart("a", "application/json", null,
				firstPartHeaders, firstPartBody);
		this.request.addPart(firstPart);

		final Map<String, List<String>> secondPartHeaders = new HashMap<>();
		final byte[] secondPartBody = "%PDF...1".getBytes();
		final Part secondPart = new MockPart("b", "application/pdf", "document.pdf",
				secondPartHeaders, secondPartBody);
		this.request.addPart(secondPart);

		final Map<String, List<String>> thirdPartHeaders = new HashMap<>();
		final byte[] thirdPartBody = "%PDF...2".getBytes();
		final Part thirdPart = new MockPart("c", "application/pdf", "attachment1.pdf",
				thirdPartHeaders, thirdPartBody);
		this.request.addPart(thirdPart);

		final Map<String, List<String>> fourthPartHeaders = new HashMap<>();
		final byte[] fourthPartBody = "%PDF...3".getBytes();
		final Part fourthPart = new MockPart("c", "application/pdf", "attachment2.pdf",
				fourthPartHeaders, fourthPartBody);
		this.request.addPart(fourthPart);

		final Map<String, List<String>> fifthPartHeaders = new HashMap<>();
		final byte[] fifthPartBody = "%PDF...4".getBytes();
		final Part fifthPart = new MockPart("c", "application/pdf", "attachment3.pdf",
				fifthPartHeaders, fifthPartBody);
		this.request.addPart(fifthPart);

		this.filter.run();

		final RequestContext ctx = RequestContext.getCurrentContext();
		assertThat(ctx.getRequest().getRequestURI()).isEqualTo("/api/foo/1");
		assertThat(ctx.getRequest().getRemoteAddr()).isEqualTo("5.6.7.8");
		assertThat(ctx.getRequest().getParts().size()).isEqualTo(5);

		final Part[] parts = ctx.getRequest().getParts().toArray(new Part[0]);
		assertThat(parts[0].getName()).isEqualTo("a");
		assertThat(parts[0].getSubmittedFileName()).isEqualTo(null);
		assertThat(parts[0].getContentType()).isEqualTo("application/json");
		assertThat(IOUtils.toByteArray(parts[0].getInputStream()))
				.isEqualTo(firstPartBody);

		assertThat(parts[1].getName()).isEqualTo("b");
		assertThat(parts[1].getSubmittedFileName()).isEqualTo("document.pdf");
		assertThat(parts[1].getContentType()).isEqualTo("application/pdf");
		assertThat(IOUtils.toByteArray(parts[1].getInputStream()))
				.isEqualTo(secondPartBody);

		assertThat(parts[2].getName()).isEqualTo("c");
		assertThat(parts[2].getSubmittedFileName()).isEqualTo("attachment1.pdf");
		assertThat(parts[2].getContentType()).isEqualTo("application/pdf");
		assertThat(IOUtils.toByteArray(parts[2].getInputStream()))
				.isEqualTo(thirdPartBody);

		assertThat(parts[3].getName()).isEqualTo("c");
		assertThat(parts[3].getSubmittedFileName()).isEqualTo("attachment2.pdf");
		assertThat(parts[3].getContentType()).isEqualTo("application/pdf");
		assertThat(IOUtils.toByteArray(parts[3].getInputStream()))
				.isEqualTo(fourthPartBody);

		assertThat(parts[4].getName()).isEqualTo("c");
		assertThat(parts[4].getSubmittedFileName()).isEqualTo("attachment3.pdf");
		assertThat(parts[4].getContentType()).isEqualTo("application/pdf");
		assertThat(IOUtils.toByteArray(parts[4].getInputStream()))
				.isEqualTo(fifthPartBody);
	}

	private class MockPart implements Part {

		private final String name;

		private final String contentType;

		private final String submittedFileName;

		private final Map<String, List<String>> headers;

		private final byte[] body;

		MockPart(final String name, final String contentType,
				final String submittedFileName, final Map<String, List<String>> headers,
				final byte[] body) {
			this.name = name;
			this.contentType = contentType;
			this.submittedFileName = submittedFileName;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.body);
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getSubmittedFileName() {
			return this.submittedFileName;
		}

		@Override
		public long getSize() {
			return this.body != null ? this.body.length : 0;
		}

		@Override
		public void write(String fileName) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void delete() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHeader(String name) {
			if (this.headers == null) {
				return null;
			}

			final List<String> values = this.headers.get(name);

			if (values == null || values.size() == 0) {
				return null;
			}

			return values.get(0);
		}

		@Override
		public Collection<String> getHeaders(String name) {
			if (this.headers == null) {
				return null;
			}

			return this.headers.get(name);
		}

		@Override
		public Collection<String> getHeaderNames() {
			if (this.headers == null) {
				return null;
			}

			return this.headers.keySet();
		}

	}

}
