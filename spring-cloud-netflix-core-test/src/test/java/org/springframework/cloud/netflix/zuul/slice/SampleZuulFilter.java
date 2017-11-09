package org.springframework.cloud.netflix.zuul.slice;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SampleZuulFilter extends ZuulFilter {
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
		return true;
	}

	@Override
	public Object run() {
		List<Pair<String, String>> responseHeaders = RequestContext.getCurrentContext()
				.getZuulResponseHeaders();
		responseHeaders.add(new Pair<>("SOME_HEADER", "SOME_VALUE"));
		return null;
	}
}
