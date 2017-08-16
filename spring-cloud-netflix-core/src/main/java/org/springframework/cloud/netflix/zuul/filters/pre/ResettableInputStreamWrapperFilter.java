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
 */
package org.springframework.cloud.netflix.zuul.filters.pre;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.common.io.ByteStreams;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * Pre {@link ZuulFilter} which prepares the request's input stream to be retryable
 *
 * @author Andre Dörnbrack
 */
public class ResettableInputStreamWrapperFilter extends ZuulFilter {
	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return RESETTABLE_INPUT_STREAM_WRAPPER_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		return hasContentLength(request) || hasFormBodyContentType(request);
	}

	private boolean hasContentLength(HttpServletRequest request) {
		return request.getContentLength() >= 0;

	}

	private boolean hasFormBodyContentType(HttpServletRequest request) {
		try {
			MediaType mediaType = MediaType.valueOf(request.getContentType());
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

		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
			private byte[] data;

			@Override
			public ServletInputStream getInputStream() throws IOException {
				if (data == null) {
					data = ByteStreams.toByteArray(super.getInputStream());
				}

				return new ResettableServletInputStreamWrapper(data);
			}
		};

		try {
			ctx.setRequest(wrapper);
			ctx.set(REQUEST_ENTITY_KEY, wrapper.getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to set request entity in request context",
					e);
		}

		return true;
	}
}
