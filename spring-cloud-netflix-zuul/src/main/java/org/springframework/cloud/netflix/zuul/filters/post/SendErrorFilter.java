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

import com.netflix.client.ClientException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.net.SocketTimeoutException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ERROR_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_ERROR_FILTER_ORDER;

/**
 * Error {@link ZuulFilter} that forwards to /error (by default) if {@link RequestContext#getThrowable()} is not null.
 *
 * @author Spencer Gibb
 */
//TODO: move to error package in Edgware
public class SendErrorFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(SendErrorFilter.class);
	protected static final String SEND_ERROR_FILTER_RAN = "sendErrorFilter.ran";

	@Value("${error.path:/error}")
	private String errorPath;

	@Override
	public String filterType() {
		return ERROR_TYPE;
	}

	@Override
	public int filterOrder() {
		return SEND_ERROR_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// only forward to errorPath if it hasn't been forwarded to already
		return ctx.getThrowable() != null
				&& !ctx.getBoolean(SEND_ERROR_FILTER_RAN, false);
	}

	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			ExceptionHolder exception = findZuulException(ctx.getThrowable());
			HttpServletRequest request = ctx.getRequest();

			request.setAttribute("javax.servlet.error.status_code", exception.getStatusCode());

			log.warn("Error during filtering", exception.getThrowable());
			request.setAttribute("javax.servlet.error.exception", exception.getThrowable());

			if (StringUtils.hasText(exception.getErrorCause())) {
				request.setAttribute("javax.servlet.error.message", exception.getErrorCause());
			}

			RequestDispatcher dispatcher = request.getRequestDispatcher(
					this.errorPath);
			if (dispatcher != null) {
				ctx.set(SEND_ERROR_FILTER_RAN, true);
				if (!ctx.getResponse().isCommitted()) {
					ctx.setResponseStatusCode(exception.getStatusCode());
					dispatcher.forward(request, ctx.getResponse());
				}
			}
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

	protected ExceptionHolder findZuulException(Throwable throwable) {
		if (throwable.getCause() instanceof ZuulRuntimeException) {
			Throwable cause = null;
			if (throwable.getCause().getCause() != null) {
				cause = throwable.getCause().getCause().getCause();
			}
			if (cause instanceof ClientException && cause.getCause() != null
					&& cause.getCause().getCause() instanceof SocketTimeoutException) {

				ZuulException zuulException = new ZuulException("", 504,
						ZuulException.class.getName() + ": Hystrix Readed time out");
				return new ZuulExceptionHolder(zuulException);
			}
			// this was a failure initiated by one of the local filters
			if(throwable.getCause().getCause() instanceof ZuulException) {
				return new ZuulExceptionHolder((ZuulException) throwable.getCause().getCause());
			}
		}
		
		if (throwable.getCause() instanceof ZuulException) {
			// wrapped zuul exception
			return  new ZuulExceptionHolder((ZuulException) throwable.getCause());
		}

		if (throwable instanceof ZuulException) {
			// exception thrown by zuul lifecycle
			return new ZuulExceptionHolder((ZuulException) throwable);
		}

		// fallback
		return new DefaultExceptionHolder(throwable);
	}

	protected interface ExceptionHolder {
		Throwable getThrowable();

	    default int getStatusCode() {
	    	return HttpStatus.INTERNAL_SERVER_ERROR.value();
		}

	    default String getErrorCause() {
	    	return null;
		}
	}

	protected static class DefaultExceptionHolder implements ExceptionHolder {
		private final Throwable throwable;

		public DefaultExceptionHolder(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public Throwable getThrowable() {
			return this.throwable;
		}
	}

	protected static class ZuulExceptionHolder implements ExceptionHolder {
		private final ZuulException exception;

		public ZuulExceptionHolder(ZuulException exception) {
			this.exception = exception;
		}

		@Override
		public Throwable getThrowable() {
			return this.exception;
		}

		@Override
		public int getStatusCode() {
			return this.exception.nStatusCode;
		}

		@Override
		public String getErrorCause() {
			return this.exception.errorCause;
		}
	}

	public void setErrorPath(String errorPath) {
		this.errorPath = errorPath;
	}

}
