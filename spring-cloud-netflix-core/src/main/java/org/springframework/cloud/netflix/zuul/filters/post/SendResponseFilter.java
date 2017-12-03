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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ReflectionUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTING_DEBUG_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_ZUUL_DEBUG_HEADER;

/**
 * Post {@link ZuulFilter} that writes responses from proxied requests to the current response.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 */
public class SendResponseFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(SendResponseFilter.class);

	private static DynamicBooleanProperty INCLUDE_DEBUG_HEADER = DynamicPropertyFactory
			.getInstance()
			.getBooleanProperty(ZuulConstants.ZUUL_INCLUDE_DEBUG_HEADER, false);

	private static DynamicIntProperty INITIAL_STREAM_BUFFER_SIZE = DynamicPropertyFactory
			.getInstance()
			.getIntProperty(ZuulConstants.ZUUL_INITIAL_STREAM_BUFFER_SIZE, 8192);

	private static DynamicBooleanProperty SET_CONTENT_LENGTH = DynamicPropertyFactory
			.getInstance()
			.getBooleanProperty(ZuulConstants.ZUUL_SET_CONTENT_LENGTH, false);
	private boolean useServlet31 = true;

	public SendResponseFilter() {
		super();
		// To support Servlet API 3.1 we need to check if setContentLengthLong exists
		try {
			//TODO: remove in 2.0
			HttpServletResponse.class.getMethod("setContentLengthLong", long.class);
		} catch(NoSuchMethodException e) {
			useServlet31 = false;
		}
	}

	/* for testing */ boolean isUseServlet31() {
		return useServlet31;
	}

	private ThreadLocal<byte[]> buffers = new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			return new byte[INITIAL_STREAM_BUFFER_SIZE.get()];
		}
	};
	
	@Override
	public String filterType() {
		return POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return SEND_RESPONSE_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext context = RequestContext.getCurrentContext();
		return context.getThrowable() == null
				&& (!context.getZuulResponseHeaders().isEmpty()
					|| context.getResponseDataStream() != null
					|| context.getResponseBody() != null);
	}

	@Override
	public Object run() {
		try {
			addResponseHeaders();
			writeResponse();
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

	private void writeResponse() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();
		// there is no body to send
		if (context.getResponseBody() == null
				&& context.getResponseDataStream() == null) {
			return;
		}
		HttpServletResponse servletResponse = context.getResponse();
		if (servletResponse.getCharacterEncoding() == null) { // only set if not set
			servletResponse.setCharacterEncoding("UTF-8");
		}
		OutputStream outStream = servletResponse.getOutputStream();
		InputStream is = null;
		try {
			if (RequestContext.getCurrentContext().getResponseBody() != null) {
				String body = RequestContext.getCurrentContext().getResponseBody();
				writeResponse(
						new ByteArrayInputStream(
								body.getBytes(servletResponse.getCharacterEncoding())),
						outStream);
				return;
			}
			boolean isGzipRequested = false;
			final String requestEncoding = context.getRequest()
					.getHeader(ZuulHeaders.ACCEPT_ENCODING);

			if (requestEncoding != null
					&& HTTPRequestUtils.getInstance().isGzipped(requestEncoding)) {
				isGzipRequested = true;
			}
			is = context.getResponseDataStream();
			InputStream inputStream = is;
			if (is != null) {
				if (context.sendZuulResponse()) {
					// if origin response is gzipped, and client has not requested gzip,
					// decompress stream
					// before sending to client
					// else, stream gzip directly to client
					if (context.getResponseGZipped() && !isGzipRequested) {
						// If origin tell it's GZipped but the content is ZERO bytes,
						// don't try to uncompress
						final Long len = context.getOriginContentLength();
						if (len == null || len > 0) {
							try {
								inputStream = new GZIPInputStream(is);
							}
							catch (java.util.zip.ZipException ex) {
								log.debug(
										"gzip expected but not "
												+ "received assuming unencoded response "
												+ RequestContext.getCurrentContext()
												.getRequest().getRequestURL()
												.toString());
								inputStream = is;
							}
						}
						else {
							// Already done : inputStream = is;
						}
					}
					else if (context.getResponseGZipped() && isGzipRequested) {
						servletResponse.setHeader(ZuulHeaders.CONTENT_ENCODING, "gzip");
					}
					writeResponse(inputStream, outStream);
				}
			}
		}
		finally {
			/**
			* We must ensure that the InputStream provided by our upstream pooling mechanism is ALWAYS closed
		 	* even in the case of wrapped streams, which are supplied by pooled sources such as Apache's
		 	* PoolingHttpClientConnectionManager. In that particular case, the underlying HTTP connection will
		 	* be returned back to the connection pool iif either close() is explicitly called, a read
			* error occurs, or the end of the underlying stream is reached. If, however a write error occurs, we will
			* end up leaking a connection from the pool without an explicit close()
			*
			* @author Johannes Edmeier
			*/
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception ex) {
					log.warn("Error while closing upstream input stream", ex);
				}
			}

			try {
				Object zuulResponse = RequestContext.getCurrentContext()
						.get("zuulResponse");
				if (zuulResponse instanceof Closeable) {
					((Closeable) zuulResponse).close();
				}
				outStream.flush();
				// The container will close the stream for us
			}
			catch (IOException ex) {
				log.warn("Error while sending response to client: " + ex.getMessage());
			}
		}
	}

	private void writeResponse(InputStream zin, OutputStream out) throws Exception {
		byte[] bytes = buffers.get();
		int bytesRead = -1;
		while ((bytesRead = zin.read(bytes)) != -1) {
			out.write(bytes, 0, bytesRead);
		}
	}

	private void addResponseHeaders() {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletResponse servletResponse = context.getResponse();
		if (INCLUDE_DEBUG_HEADER.get()) {
			@SuppressWarnings("unchecked")
			List<String> rd = (List<String>) context.get(ROUTING_DEBUG_KEY);
			if (rd != null) {
				StringBuilder debugHeader = new StringBuilder();
				for (String it : rd) {
					debugHeader.append("[[[" + it + "]]]");
				}
				servletResponse.addHeader(X_ZUUL_DEBUG_HEADER, debugHeader.toString());
			}
		}
		List<Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();
		if (zuulResponseHeaders != null) {
			for (Pair<String, String> it : zuulResponseHeaders) {
				servletResponse.addHeader(it.first(), it.second());
			}
		}
		// Only inserts Content-Length if origin provides it and origin response is not
		// gzipped
		if (SET_CONTENT_LENGTH.get()) {
			Long contentLength = context.getOriginContentLength();
			if ( contentLength != null && !context.getResponseGZipped()) {
				if(useServlet31) {
					servletResponse.setContentLengthLong(contentLength);
				} else {
					//Try and set some kind of content length if we can safely convert the Long to an int
					if (isLongSafe(contentLength)) {
						servletResponse.setContentLength(contentLength.intValue());
					}
				}
			}
		}
	}

	private boolean isLongSafe(long value) {
		return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE;
	}

}
