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

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.DispatcherServlet;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ServletInputStreamWrapper;

/**
 * @author Spencer Gibb
 */
public class FormBodyWrapperFilter extends ZuulFilter {

	private Field requestField;
	private Field servletRequestField;

	public FormBodyWrapperFilter() {
		this.requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class,
				"req", HttpServletRequest.class);
		this.servletRequestField = ReflectionUtils.findField(ServletRequestWrapper.class,
				"request", ServletRequest.class);
		Assert.notNull(this.requestField,
				"HttpServletRequestWrapper.req field not found");
		Assert.notNull(this.servletRequestField,
				"ServletRequestWrapper.request field not found");
		this.requestField.setAccessible(true);
		this.servletRequestField.setAccessible(true);
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return -1;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		String contentType = request.getContentType();
		// Don't use this filter on GET method
		if (contentType == null) {
			return false;
		}
		// Only use this filter for form data and only for multipart data in a
		// DispatcherServlet handler
		try {
			MediaType mediaType = MediaType.valueOf(contentType);
			return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
					|| (isDispatcherServletRequest(request)
							&& MediaType.MULTIPART_FORM_DATA.includes(mediaType));
		}
		catch (InvalidMediaTypeException ex) {
			return false;
		}
	}

	private boolean isDispatcherServletRequest(HttpServletRequest request) {
		return request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		FormBodyRequestWrapper wrapper = null;
		if (request instanceof HttpServletRequestWrapper) {
			HttpServletRequest wrapped = (HttpServletRequest) ReflectionUtils
					.getField(this.requestField, request);
			wrapper = new FormBodyRequestWrapper(wrapped);
			ReflectionUtils.setField(this.requestField, request, wrapper);
			if (request instanceof ServletRequestWrapper) {
				ReflectionUtils.setField(this.servletRequestField, request, wrapper);
			}
		}
		else {
			wrapper = new FormBodyRequestWrapper(request);
			ctx.setRequest(wrapper);
		}
		if (wrapper != null) {
			ctx.getZuulRequestHeaders().put("content-type", wrapper.getContentType());
		}
		return null;
	}

	private class FormBodyRequestWrapper extends Servlet30RequestWrapper {

		private HttpServletRequest request;

		private byte[] contentData;

		private MediaType contentType;

		private int contentLength;

		private AllEncompassingFormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();

		public FormBodyRequestWrapper(HttpServletRequest request) {
			super(request);
			this.request = request;
		}

		@Override
		public String getContentType() {
			if (this.contentData == null) {
				buildContentData();
			}
			return this.contentType.toString();
		}

		@Override
		public int getContentLength() {
			if (super.getContentLength() <= 0) {
				return super.getContentLength();
			}
			if (this.contentData == null) {
				buildContentData();
			}
			return this.contentLength;
		}

		public long getContentLengthLong() {
			return getContentLength();
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (this.contentData == null) {
				buildContentData();
			}
			return new ServletInputStreamWrapper(this.contentData);
		}

		private synchronized void buildContentData() {
			try {
				MultiValueMap<String, Object> builder = new LinkedMultiValueMap<String, Object>();
				Set<String> queryParams = findQueryParams();
				if (! (this.request instanceof MultipartRequest)) {
					for (Entry<String, String[]> entry : this.request.getParameterMap()
							.entrySet()) {
						if (!queryParams.contains(entry.getKey())) {
							for (String value : entry.getValue()) {
								builder.add(entry.getKey(), value);
							}
						}
					}
				}
				if (this.request instanceof MultipartRequest) {
					MultipartRequest multi = (MultipartRequest) this.request;
					for (Entry<String, String[]> entry : this.request.getParameterMap()
							.entrySet()) {
						if (!queryParams.contains(entry.getKey())) {
							HttpHeaders headers = new HttpHeaders();
							if(multi.getMultipartContentType(entry.getKey()) != null){
								headers.setContentType(MediaType.valueOf(
										multi.getMultipartContentType(entry.getKey())));
							}
							for (String value : entry.getValue()) {
								HttpEntity<String> entity = new HttpEntity<String>(value, headers);
								builder.add(entry.getKey(), entity);
							}
						}
					}

					for (Entry<String, List<MultipartFile>> parts : multi
							.getMultiFileMap().entrySet()) {
						for (MultipartFile file : parts.getValue()) {
							HttpHeaders headers = new HttpHeaders();
							headers.setContentDispositionFormData(file.getName(),
									file.getOriginalFilename());
							if (file.getContentType() != null) {
								headers.setContentType(
										MediaType.valueOf(file.getContentType()));
							}
							HttpEntity<Resource> entity = new HttpEntity<Resource>(
									new InputStreamResource(file.getInputStream()),
									headers);
							builder.add(parts.getKey(), entity);
						}
					}
				}
				FormHttpOutputMessage data = new FormHttpOutputMessage();
				this.contentType = MediaType.valueOf(this.request.getContentType());
				data.getHeaders().setContentType(this.contentType);
				this.converter.write(builder, this.contentType, data);
				// copy new content type including multipart boundary
				this.contentType = data.getHeaders().getContentType();
				this.contentData = data.getInput();
				this.contentLength = this.contentData.length;
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot convert form data", e);
			}
		}

		private Set<String> findQueryParams() {
			Set<String> result = new HashSet<>();
			String query = this.request.getQueryString();
			if (query != null) {
				for (String value : StringUtils.tokenizeToStringArray(query, "&")) {
					if (value.contains("=")) {
						value = value.substring(0, value.indexOf("="));
					}
					result.add(value);
				}
			}
			return result;
		}

		private class FormHttpOutputMessage implements HttpOutputMessage {

			private HttpHeaders headers = new HttpHeaders();
			private ByteArrayOutputStream output = new ByteArrayOutputStream();

			@Override
			public HttpHeaders getHeaders() {
				return this.headers;
			}

			@Override
			public OutputStream getBody() throws IOException {
				return this.output;
			}

			public byte[] getInput() throws IOException {
				this.output.flush();
				return this.output.toByteArray();
			}

		}

	}

}
