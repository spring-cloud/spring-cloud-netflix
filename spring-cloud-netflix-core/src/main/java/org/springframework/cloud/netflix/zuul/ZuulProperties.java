package org.springframework.cloud.netflix.zuul;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {
	private String prefix = "";
	private boolean stripPrefix = true;
	private Map<String, ZuulRoute> routes = new LinkedHashMap<String, ZuulRoute>();
	private boolean addProxyHeaders = true;
	private List<String> ignoredServices = new ArrayList<String>();

	@PostConstruct
	public void init() {
		for (Entry<String, ZuulRoute> entry : this.routes.entrySet()) {
			ZuulRoute value = entry.getValue();
			if (!StringUtils.hasText(value.getLocation())) {
				value.serviceId = entry.getKey();
			}
			if (!StringUtils.hasText(value.getId())) {
				value.id = entry.getKey();
			}
			if (!StringUtils.hasText(value.getPath())) {
				value.path = "/" + entry.getKey() + "/**";
			}
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZuulRoute {
		private String id;
		private String path;
		private String serviceId;
		private String url;
		private boolean stripPrefix = true;

		public ZuulRoute(String text) {
			String location = null;
			String path = text;
			if (text.contains("=")) {
				String[] values = StringUtils.trimArrayElements(StringUtils.split(text,
						"="));
				location = values[1];
				path = values[0];
			}
			this.id = extractId(path);
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			setLocation(location);
			this.path = path;
		}

		public ZuulRoute(String path, String location) {
			this.id = extractId(path);
			this.path = path;
			setLocation(location);
		}

		public String getLocation() {
			if (StringUtils.hasText(url)) {
				return url;
			}
			return serviceId;
		}

		public void setLocation(String location) {
			if (location != null
					&& (location.startsWith("http:") || location.startsWith("https:"))) {
				url = location;
			}
			else {
				serviceId = location;
			}
		}

		private String extractId(String path) {
			path = path.startsWith("/") ? path.substring(1) : path;
			path = path.replace("/*", "").replace("*", "");
			return path;
		}
	}

}
