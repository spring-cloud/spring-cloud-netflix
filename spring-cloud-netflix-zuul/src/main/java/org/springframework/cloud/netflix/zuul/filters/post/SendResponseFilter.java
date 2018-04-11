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
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.ReflectionUtils;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
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

	private boolean useServlet31 = true;
	private ZuulProperties zuulProperties;

	private ThreadLocal<byte[]> buffers;

	@Deprecated
	public SendResponseFilter() {
		this(new ZuulProperties());
	}

	public SendResponseFilter(ZuulProperties zuulProperties) {
		this.zuulProperties = zuulProperties;
		// To support Servlet API 3.1 we need to check if setContentLengthLong exists
		// minimum support in Spring 5 is 3.0 so we need to keep tihs
		try {
			HttpServletResponse.class.getMethod("setContentLengthLong", long.class);
		} catch(NoSuchMethodException e) {
			useServlet31 = false;
		}
		buffers = ThreadLocal.withInitial(() -> new byte[zuulProperties.getInitialStreamBufferSize()]);
	}

	/* for testing */ boolean isUseServlet31() {
		return useServlet31;
	}

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
			if (context.getResponseBody() != null) {
				String body = context.getResponseBody();
				is = new ByteArrayInputStream(
								body.getBytes(servletResponse.getCharacterEncoding()));
			}
			else {
				is = context.getResponseDataStream();
				if (is!=null && context.getResponseGZipped()) {
					// if origin response is gzipped, and client has not requested gzip,
					// decompress stream before sending to client
					// else, stream gzip directly to client
					if (isGzipRequested(context)) {
						servletResponse.setHeader(ZuulHeaders.CONTENT_ENCODING, "gzip");
					}
					else {
						is = handleGzipStream(is);
					}
				}
			}
			
			if (is!=null) {
				writeResponse(is, outStream);
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
				Object zuulResponse = context.get("zuulResponse");
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

	
	protected InputStream handleGzipStream(InputStream in) throws Exception {
		// Record bytes read during GZip initialization to allow to rewind the stream if needed
		//
		RecordingInputStream stream = new RecordingInputStream(in);
		try {
			return new GZIPInputStream(stream);
		}
		catch (java.util.zip.ZipException | java.io.EOFException ex) {
			
			if (stream.getBytesRead()==0) {
				// stream was empty, return the original "empty" stream
				return in;
			}
			else {
				// reset the stream and assume an unencoded response
				log.warn(
						"gzip response expected but failed to read gzip headers, assuming unencoded response for request "
							+ RequestContext.getCurrentContext()
							.getRequest().getRequestURL()
							.toString());

				stream.reset();
				return stream;
			}
		}
		finally {
			stream.stopRecording();
		}
	}

	
	protected boolean isGzipRequested(RequestContext context) {
		final String requestEncoding = context.getRequest()
				.getHeader(ZuulHeaders.ACCEPT_ENCODING);

		return requestEncoding != null
				&& HTTPRequestUtils.getInstance().isGzipped(requestEncoding);
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
		if (this.zuulProperties.isIncludeDebugHeader()) {
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
		if (includeContentLengthHeader(context)) {
			Long contentLength = context.getOriginContentLength();
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

	private boolean isLongSafe(long value) {
		return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE;
	}

	protected boolean includeContentLengthHeader(RequestContext context) {
		// Not configured to forward the header
		if (!this.zuulProperties.isSetContentLength()) {
			return false;
		}
		
		// Only if Content-Length is provided
		if (context.getOriginContentLength() == null) {
			return false;
		}
		
		// If response is compressed, include header only if we are not about to decompress it
		if (context.getResponseGZipped()) {
			return context.isGzipRequested();
		}
		
		// Forward it in all other cases
		return true;
	}
	
	
	/**
	 * InputStream recording bytes read to allow for a reset() until recording is stopped.
	 */
	private static class RecordingInputStream extends InputStream {

		private InputStream delegate;
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		public RecordingInputStream(InputStream delegate) {
			super();
			this.delegate = Objects.requireNonNull(delegate);
		}
		
		@Override
		public int read() throws IOException {
			int read = delegate.read();
			
			if (buffer!=null && read!=-1) {
				buffer.write(read);
			}
			
			return read;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = delegate.read(b, off, len);
			
			if (buffer!=null && read!=-1) {
				buffer.write(b, off, read);
			}
			
			return read;
		}
		
		public void reset() {
			if (buffer==null) {
				throw new IllegalStateException("Stream is not recording");
			}

			this.delegate = new SequenceInputStream(new ByteArrayInputStream(buffer.toByteArray()), delegate);
			this.buffer = new ByteArrayOutputStream();
		}

		public int getBytesRead() {
			return (buffer==null)?-1:buffer.size();
		}
		
		public void stopRecording() {
			this.buffer = null;
		}
		
		@Override
		public void close() throws IOException {
			this.delegate.close();
		}
	}
}
