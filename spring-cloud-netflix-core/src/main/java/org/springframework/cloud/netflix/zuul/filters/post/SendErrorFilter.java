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

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import java.lang.reflect.UndeclaredThrowableException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
		return "error";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// only forward to errorPath if it hasn't been forwarded to already
		return !ctx.getBoolean(SEND_ERROR_FILTER_RAN, false) && ctx.getThrowable() != null;
	}

	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			HttpServletRequest request = ctx.getRequest();

			ErrorResponse errorResponse = getErrorResponse(ctx);

			request.setAttribute("javax.servlet.error.status_code", errorResponse.code);
			request.setAttribute("javax.servlet.error.message", errorResponse.message);
			request.setAttribute("javax.servlet.error.exception", errorResponse.throwable);

			RequestDispatcher dispatcher = request.getRequestDispatcher(
					this.errorPath);
			if (dispatcher != null) {
				ctx.set(SEND_ERROR_FILTER_RAN, true);
				if (!ctx.getResponse().isCommitted()) {
					dispatcher.forward(request, ctx.getResponse());
				}
			}
		} catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

	private ErrorResponse getErrorResponse(RequestContext ctx) {
		Throwable ex = ctx.getThrowable();

		int idx = ExceptionUtils.indexOfType(ex, UndeclaredThrowableException.class);
		if (idx == -1) { // UndeclaredThrowableException was not found in stacktrace
			return new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
		UndeclaredThrowableException undeclared = (UndeclaredThrowableException) ExceptionUtils.getThrowables(ex)[idx];
		Throwable cause = undeclared.getUndeclaredThrowable();
		if (cause instanceof ZuulException) {
			ZuulException zuulException = (ZuulException) undeclared.getUndeclaredThrowable();
			return new ErrorResponse(zuulException.nStatusCode, zuulException.errorCause, zuulException);
		}
		return new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause.getMessage(), cause);
	}

	public void setErrorPath(String errorPath) {
		this.errorPath = errorPath;
	}

	private static class ErrorResponse {

		public final int code;
		public final String message;
		public final Throwable throwable;

		public ErrorResponse(int code, String message, Throwable throwable) {
			this.code = code;
			this.message = message;
			this.throwable = throwable;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
					.append("code", code)
					.append("message", message)
					.append("throwable", throwable)
					.toString();
		}
	}
}
