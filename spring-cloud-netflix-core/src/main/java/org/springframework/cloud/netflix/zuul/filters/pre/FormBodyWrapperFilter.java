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

import com.google.common.base.Throwables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ServletInputStreamWrapper;

/**
 * @author Spencer Gibb
 */
public class FormBodyWrapperFilter extends ZuulFilter {
	protected Field requestField = null;

	public FormBodyWrapperFilter() {
		requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "req",
				HttpServletRequest.class);
		Assert.notNull(requestField, "HttpServletRequestWrapper.req field not found");
		requestField.setAccessible(true);
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

		//Don't use this filter on GET method
		if(contentType == null) {
			return false;
		}

		//Only use this filter for MediaType : application/x-www-form-urlencoded
		try {
			return MediaType.APPLICATION_FORM_URLENCODED.includes(MediaType.valueOf(contentType));
		} catch (InvalidMediaTypeException imte) {
			return false;
		}
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		if (request instanceof HttpServletRequestWrapper) {
			try {
				HttpServletRequest wrapped = (HttpServletRequest) requestField.get(request);
				requestField.set(request, new FormBodyRequestWrapper(wrapped));
			}
			catch (IllegalAccessException e) {
				Throwables.propagate(e);
			}
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
			if (contentData == null) {
				contentData = buildContentData();
			}
			return contentData.length;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (RequestContext.getCurrentContext().isChunkedRequestBody()) {
				return request.getInputStream();
			}
			else {
				if (contentData == null) {
					contentData = buildContentData();
				}
				return new ServletInputStreamWrapper(contentData);
			}
		}

		private byte[] buildContentData() {
			StringBuilder builder = new StringBuilder();
			for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
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
