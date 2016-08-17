/*
 * Copyright 2013-2016 the original author or authors.
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

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class SendErrorFilter extends ZuulFilter {

	protected static final String SEND_ERROR_FILTER_RAN = "sendErrorFilter.ran";

	@Value("${error.path:/error}")
	private String errorPath;

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// only forward to errorPath if it hasn't been forwarded to already
		return ctx.containsKey("error.status_code")
				&& !ctx.getBoolean(SEND_ERROR_FILTER_RAN, false);
	}

	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			HttpServletRequest request = ctx.getRequest();

			int statusCode = (Integer) ctx.get("error.status_code");
			request.setAttribute("javax.servlet.error.status_code", statusCode);

			if (ctx.containsKey("error.exception")) {
				Object e = ctx.get("error.exception");
				log.warn("Error during filtering", Throwable.class.cast(e));
				request.setAttribute("javax.servlet.error.exception", e);
			}

			if (ctx.containsKey("error.message")) {
				String message = (String) ctx.get("error.message");
				request.setAttribute("javax.servlet.error.message", message);
			}

			doOnError(ctx, request);
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

	/**
	 * Dispatches to error path
	 */
	protected void doOnError(RequestContext ctx, HttpServletRequest request) throws ServletException, IOException {
		RequestDispatcher dispatcher = request.getRequestDispatcher(
				this.errorPath);
		if (dispatcher != null) {
			ctx.set(SEND_ERROR_FILTER_RAN, true);
			if (!ctx.getResponse().isCommitted()) {
				dispatcher.forward(request, ctx.getResponse());
			}
		}
	}

	public void setErrorPath(String errorPath) {
		this.errorPath = errorPath;
	}

}
