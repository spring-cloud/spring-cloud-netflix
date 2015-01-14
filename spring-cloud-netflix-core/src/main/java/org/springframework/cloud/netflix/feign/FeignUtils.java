package org.springframework.cloud.netflix.feign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * @author Spencer Gibb
 */
public class FeignUtils {
	static HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
		HttpHeaders httpHeaders = new HttpHeaders();
		for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
			httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return httpHeaders;
	}
}
