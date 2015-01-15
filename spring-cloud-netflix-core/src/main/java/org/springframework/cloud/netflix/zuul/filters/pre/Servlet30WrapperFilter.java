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
import java.util.Collection;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.Throwables;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;

/**
 * @author Spencer Gibb
 */
public class Servlet30WrapperFilter extends ZuulFilter {

	protected Field requestField = null;

	public Servlet30WrapperFilter() {
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
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		return true; // TODO: only if in servlet 3.0 env
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		if (request instanceof HttpServletRequestWrapper) {
			try {
				request = (HttpServletRequest) this.requestField.get(request);
			}
			catch (IllegalAccessException ex) {
				Throwables.propagate(ex);
			}
		}
		ctx.setRequest(new Servlet30RequestWrapper(request));
		// ctx.setResponse(new HttpServletResponseWrapper(ctx.getResponse()));
		return null;
	}

	private class Servlet30RequestWrapper extends HttpServletRequestWrapper {
		private HttpServletRequest request;

		Servlet30RequestWrapper(HttpServletRequest request) {
			super(request);
			this.request = request;
		}

		@Override
		public boolean authenticate(HttpServletResponse response) throws IOException,
				ServletException {
			return this.request.authenticate(response);
		}

		@Override
		public void login(String username, String password) throws ServletException {
			this.request.login(username, password);
		}

		@Override
		public void logout() throws ServletException {
			this.request.logout();
		}

		@Override
		public Collection<Part> getParts() throws IOException, IllegalStateException,
				ServletException {
			return this.request.getParts();
		}

		@Override
		public Part getPart(String name) throws IOException, IllegalStateException,
				ServletException {
			return this.request.getPart(name);
		}

		@Override
		public ServletContext getServletContext() {
			return this.request.getServletContext();
		}

		@Override
		public AsyncContext startAsync() {
			return this.request.startAsync();
		}

		@Override
		public AsyncContext startAsync(ServletRequest servletRequest,
				ServletResponse servletResponse) {
			return this.request.startAsync(servletRequest, servletResponse);
		}

		@Override
		public boolean isAsyncStarted() {
			return this.request.isAsyncStarted();
		}

		@Override
		public boolean isAsyncSupported() {
			return this.request.isAsyncSupported();
		}

		@Override
		public AsyncContext getAsyncContext() {
			return this.request.getAsyncContext();
		}

		@Override
		public DispatcherType getDispatcherType() {
			return this.request.getDispatcherType();
		}

	}

}
