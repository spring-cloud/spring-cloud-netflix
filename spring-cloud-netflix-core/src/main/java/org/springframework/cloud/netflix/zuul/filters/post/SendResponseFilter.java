package org.springframework.cloud.netflix.zuul.filters.post;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Throwables;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;

public class SendResponseFilter extends ZuulFilter {

	static DynamicBooleanProperty INCLUDE_DEBUG_HEADER = DynamicPropertyFactory
			.getInstance().getBooleanProperty(ZuulConstants.ZUUL_INCLUDE_DEBUG_HEADER,
					false);

	static DynamicIntProperty INITIAL_STREAM_BUFFER_SIZE = DynamicPropertyFactory
			.getInstance().getIntProperty(ZuulConstants.ZUUL_INITIAL_STREAM_BUFFER_SIZE,
					1024);

	static DynamicBooleanProperty SET_CONTENT_LENGTH = DynamicPropertyFactory
			.getInstance().getBooleanProperty(ZuulConstants.ZUUL_SET_CONTENT_LENGTH,
					false);

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 1000;
	}

	@Override
	public boolean shouldFilter() {
		return !RequestContext.getCurrentContext().getZuulResponseHeaders().isEmpty()
				|| RequestContext.getCurrentContext().getResponseDataStream() != null
				|| RequestContext.getCurrentContext().getResponseBody() != null;
	}

	@Override
	public Object run() {
		try {
			addResponseHeaders();
			writeResponse();
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
		return null;
	}

	void writeResponse() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();

		// there is no body to send
		if (context.getResponseBody() == null && context.getResponseDataStream() == null) {
			return;
		}

		HttpServletResponse servletResponse = context.getResponse();
		servletResponse.setCharacterEncoding("UTF-8");

		OutputStream outStream = servletResponse.getOutputStream();
		InputStream is = null;
		try {
			if (RequestContext.getCurrentContext().getResponseBody() != null) {
				String body = RequestContext.getCurrentContext().getResponseBody();
				writeResponse(new ByteArrayInputStream(body.getBytes()), outStream);
				return;
			}

			boolean isGzipRequested = false;
			final String requestEncoding = context.getRequest().getHeader(
					ZuulHeaders.ACCEPT_ENCODING);
			if (requestEncoding != null && requestEncoding.equals("gzip")) {
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
						try {
							inputStream = new GZIPInputStream(is);

						}
						catch (java.util.zip.ZipException e) {
							System.out
									.println("gzip expected but not received assuming unencoded response"
											+ RequestContext.getCurrentContext()
													.getRequest().getRequestURL()
													.toString());
							inputStream = is;
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
			try {
				if (is != null) {
					is.close();
				}

				outStream.flush();
				outStream.close();
			}
			catch (IOException e) {

			}
		}
	}

	private void writeResponse(InputStream zin, OutputStream out) throws Exception {
		byte[] bytes = new byte[INITIAL_STREAM_BUFFER_SIZE.get()];
		int bytesRead = -1;
		while ((bytesRead = zin.read(bytes)) != -1) {
			// if (Debug.debugRequest() && !Debug.debugRequestHeadersOnly()) {
			// Debug.addRequestDebug("OUTBOUND: <  " + new String(bytes, 0, bytesRead));
			// }

			try {
				out.write(bytes, 0, bytesRead);
				out.flush();
			}
			catch (IOException e) {
				// ignore
				e.printStackTrace();
			}

			// doubles buffer size if previous read filled it
			if (bytesRead == bytes.length) {
				bytes = new byte[bytes.length * 2];
			}
		}
	}

	private void addResponseHeaders() {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletResponse servletResponse = context.getResponse();
		List<Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();

		@SuppressWarnings("unchecked")
		List<String> rd = (List<String>) RequestContext.getCurrentContext().get(
				"routingDebug");
		if (rd != null) {
			StringBuilder debugHeader = new StringBuilder();
			for (String it : rd) {
				debugHeader.append("[[[" + it + "]]]");
			}
			if (INCLUDE_DEBUG_HEADER.get()) {
				servletResponse.addHeader("X-Zuul-Debug-Header", debugHeader.toString());
			}
		}

		if (zuulResponseHeaders != null) {
			for (Pair<String, String> it : zuulResponseHeaders) {
				servletResponse.addHeader(it.first(), it.second());
			}
		}

		RequestContext ctx = RequestContext.getCurrentContext();
		Integer contentLength = ctx.getOriginContentLength();

		// Only inserts Content-Length if origin provides it and origin response is not
		// gzipped
		if (SET_CONTENT_LENGTH.get()) {
			if (contentLength != null && !ctx.getResponseGZipped()) {
				servletResponse.setContentLength(contentLength);
			}
		}
	}

}