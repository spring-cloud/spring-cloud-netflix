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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map.Entry;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.Charsets;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ServletInputStreamWrapper;

/**
 * @author Spencer Gibb
 */
public class FormBodyWrapperFilter extends ZuulFilter {

	private Field requestField;

	public FormBodyWrapperFilter() {
		this.requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class,
				"req", HttpServletRequest.class);
		Assert.notNull(this.requestField, "HttpServletRequestWrapper.req field not found");
		this.requestField.setAccessible(true);
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
		// Only use this filter for MediaType : application/x-www-form-urlencoded
		try {
			return MediaType.APPLICATION_FORM_URLENCODED.includes(MediaType
					.valueOf(contentType));
		}
		catch (InvalidMediaTypeException ex) {
			return false;
		}
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		if (request instanceof HttpServletRequestWrapper) {
			HttpServletRequest wrapped = (HttpServletRequest) ReflectionUtils.getField(
					this.requestField, request);
			ReflectionUtils.setField(this.requestField, request,
					new FormBodyRequestWrapper(wrapped));
		}
		else {
			ctx.setRequest(new FormBodyRequestWrapper(request));
		}
		return null;
	}

	private class FormBodyRequestWrapper extends HttpServletRequestWrapper {

		private HttpServletRequest request;

		private byte[] contentData;

		public FormBodyRequestWrapper(HttpServletRequest request) {
			super(request);
			this.request = request;
		}

		@Override
		public int getContentLength() {
			if (this.contentData == null) {
				this.contentData = buildContentData();
			}
			return this.contentData.length;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (RequestContext.getCurrentContext().isChunkedRequestBody()) {
				return this.request.getInputStream();
			}
			else {
				if (this.contentData == null) {
					this.contentData = buildContentData();
				}
				return new ServletInputStreamWrapper(this.contentData);
			}
		}

		private byte[] buildContentData() {
			StringBuilder builder = new StringBuilder();
			for (Entry<String, String[]> entry : this.request.getParameterMap()
					.entrySet()) {
				for (String value : entry.getValue()) {
					if (builder.length() != 0) {
						builder.append("&");
					}
					builder.append(entry.getKey()).append("=").append(value);
				}
			}
			return builder.toString().getBytes(Charsets.UTF_8);
		}

	}

}
