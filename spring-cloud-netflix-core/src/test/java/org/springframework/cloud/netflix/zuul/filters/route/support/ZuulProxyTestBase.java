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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.RoutesMvcEndpoint;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.support.RibbonRetryIntegrationTestBase.RetryableTestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeThat;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public abstract class ZuulProxyTestBase {

	@Value("${local.server.port}")
	protected int port;

	@Autowired
	protected DiscoveryClientRouteLocator routes;

	@Autowired
	protected RoutesMvcEndpoint endpoint;

	@Autowired
	protected RibbonCommandFactory<?> ribbonCommandFactory;

	@Autowired
	protected MyErrorController myErrorController;

	@Before
	public void cleanup() {
		this.myErrorController.clear();
	}

	@Before
	public void setTestRequestcontext() {
		RequestContext.testSetCurrentContext(null);
		RequestContext.getCurrentContext().unset();
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	/**
	 * used to disable patch tests if client doesn't support it
	 */
	protected boolean supportsPatch() {
		return true;
	}

	/**
	 * used to switch delete tests with a boyd if client doesn't support it
	 */
	protected boolean supportsDeleteWithBody() {
		return true;
	}

	protected String getRoute(String path) {
		for (Route route : this.routes.getRoutes()) {
			if (path.equals(route.getFullPath())) {
				return route.getLocation();
			}
		}
		return null;
	}

	@Test
	public void bindRouteUsingPhysicalRoute() {
		assertEquals("http://localhost:7777/local", getRoute("/test/**"));
	}

	@Test
	public void bindRouteUsingOnlyPath() {
		assertEquals("simple", getRoute("/simple/**"));
	}

	@Test
	public void getOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted 1!", result.getBody());
	}

	@Test
	public void stripPrefixFalseAppendsPath() {
		this.routes.addRoute(new ZuulProperties.ZuulRoute("strip", "/strip/**", "strip",
				"http://localhost:" + this.port + "/local", false, false, null));
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/strip", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		// Prefix not stripped to it goes to /local/strip
		assertEquals("Gotten strip!", result.getBody());
	}

	@Test
	public void testNotFoundFromApp() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/notfound",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void testNotFoundOnProxy() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/myinvalidpath", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void getSecondLevel() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/another/twolevel/local/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void ribbonRouteWithSpace() {
		String uri = "/simple/spa ce";
		this.myErrorController.setUriToMatch(uri);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
		assertFalse(myErrorController.wasControllerUsed());
	}

	@Test
	public void ribbonDeleteWithBody() {
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/deletewithbody",
				HttpMethod.DELETE, new HttpEntity<>("deleterequestbody"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		if (supportsDeleteWithBody()) {
			assertEquals("Deleted deleterequestbody", result.getBody());
		}
		else {
			assertEquals("Deleted null", result.getBody());
		}
	}

	@Test
	public void ribbonRouteWithNonExistentUri() {
		String uri = "/simple/nonExistent";
		this.myErrorController.setUriToMatch(uri);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		assertFalse(myErrorController.wasControllerUsed());
	}

	@Test
	public void simpleHostRouteWithSpace() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/spa ce", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOriginalQueryString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?original=value1&original=value2",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {original=[value1, value2]}", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOverriddenQString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?override=true&different=key",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {key=[overridden]}", result.getBody());
	}

	@Test
	public void patchOnSelfViaSimpleHostRoutingFilter() {
		assumeThat(supportsPatch(), is(true));

		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.PATCH,
				new HttpEntity<>("TestPatch"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Patched 1!", result.getBody());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void javascriptEncodedFormParams() {
		TestRestTemplate testRestTemplate = new TestRestTemplate();
		ArrayList<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.addAll(Arrays.asList(new StringHttpMessageConverter(),
				new NoEncodingFormHttpMessageConverter()));
		testRestTemplate.getRestTemplate().setMessageConverters(converters);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("foo", "(bar)");
		ResponseEntity<String> result = testRestTemplate.postForEntity(
				"http://localhost:" + this.port + "/simple/local", map, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted [(bar)] and Content-Length was: 13!", result.getBody());
	}

	public static abstract class AbstractZuulProxyApplication
			extends DelegatingWebMvcConfiguration {

		private final Log LOG = LogFactory.getLog(RetryableTestConfig.class);

		@RequestMapping(value = "/local/{id}", method = RequestMethod.PATCH)
		public String patch(@PathVariable final String id,
				@RequestBody final String body) {
			return "Patched " + id + "!";
		}

		@RequestMapping("/testing123")
		public String testing123() {
			throw new RuntimeException("myerror");
		}

		@RequestMapping("/local")
		public String local() {
			return "Hello local";
		}

		@RequestMapping(value = "/local", method = RequestMethod.POST)
		public String postWithFormParam(HttpServletRequest request,
				@RequestBody MultiValueMap<String, String> body) {
			return "Posted " + body.get("foo") + " and Content-Length was: "
					+ request.getContentLength() + "!";
		}

		@RequestMapping(value = "/deletewithbody", method = RequestMethod.DELETE)
		public String deleteWithBody(@RequestBody(required = false) String body) {
			return "Deleted " + body;
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.DELETE)
		public String delete(@PathVariable String id) {
			return "Deleted " + id + "!";
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
		public ResponseEntity<?> get(@PathVariable String id) {
			if ("notfound".equalsIgnoreCase(id)) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok("Gotten " + id + "!");
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.POST)
		public String post(@PathVariable String id, @RequestBody String body) {
			return "Posted " + id + "!";
		}

		@RequestMapping(value = "/qstring")
		public String qstring(@RequestParam MultiValueMap<String, String> params) {
			return "Received " + params.toString();
		}

		@RequestMapping("/")
		public String home() {
			return "Hello world";
		}

		@RequestMapping("/spa ce")
		public String space() {
			return "Hello space";
		}

		@RequestMapping("/slow")
		public String slow() {
			try {
				Thread.sleep(80000);
			}
			catch (InterruptedException e) {
				LOG.info(e);
				Thread.currentThread().interrupt();
			}
			return "slow";
		}

		@Bean
		public ZuulFallbackProvider fallbackProvider() {
			return new FallbackProvider();
		}

		@Bean
		public ZuulFilter sampleFilter() {
			return new ZuulFilter() {
				@Override
				public String filterType() {
					return PRE_TYPE;
				}

				@Override
				public boolean shouldFilter() {
					return true;
				}

				@Override
				public Object run() {
					if (RequestContext.getCurrentContext().getRequest().getParameterMap()
							.containsKey("override")) {
						Map<String, List<String>> overridden = new HashMap<>();
						overridden.put("key", Arrays.asList("overridden"));
						RequestContext.getCurrentContext()
								.setRequestQueryParams(overridden);
					}
					return null;
				}

				@Override
				public int filterOrder() {
					return 0;
				}
			};

		}

		@Override
		public RequestMappingHandlerMapping requestMappingHandlerMapping() {
			RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();
			mapping.setRemoveSemicolonContent(false);
			return mapping;
		}

	}

	public static class FallbackProvider implements ZuulFallbackProvider {

		@Override
		public String getRoute() {
			return "simple";
		}

		@Override
		public ClientHttpResponse fallbackResponse() {
			return new ClientHttpResponse() {
				@Override
				public HttpStatus getStatusCode() throws IOException {
					return HttpStatus.OK;
				}

				@Override
				public int getRawStatusCode() throws IOException {
					return 200;
				}

				@Override
				public String getStatusText() throws IOException {
					return null;
				}

				@Override
				public void close() {

				}

				@Override
				public InputStream getBody() throws IOException {
					return new ByteArrayInputStream("fallback".getBytes());
				}

				@Override
				public HttpHeaders getHeaders() {
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.TEXT_HTML);
					return headers;
				}
			};
		}
	}

	@Configuration
	public class FormEncodedMessageConverterConfiguration
			extends WebMvcConfigurerAdapter {

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			FormHttpMessageConverter converter = new FormHttpMessageConverter();
			MediaType mediaType = new MediaType("application", "x-www-form-urlencoded",
					Charset.forName("UTF-8"));
			converter.setSupportedMediaTypes(Arrays.asList(mediaType));
			converters.add(converter);
			super.configureMessageConverters(converters);
		}
	}

	// Load balancer with fixed server list for "simple" pointing to localhost
	@Configuration
	public static class SimpleRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

	@Configuration
	public static class AnotherRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

	public static class MyErrorController extends BasicErrorController {
		ThreadLocal<String> uriToMatch = new ThreadLocal<>();

		AtomicBoolean controllerUsed = new AtomicBoolean();

		public MyErrorController(ErrorAttributes errorAttributes) {
			super(errorAttributes, new ErrorProperties());
		}

		@Override
		public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
			String errorUri = (String) request
					.getAttribute("javax.servlet.error.request_uri");

			if (errorUri != null && errorUri.equals(this.uriToMatch.get())) {
				controllerUsed.set(true);
			}
			this.uriToMatch.remove();
			return super.error(request);
		}

		public void setUriToMatch(String uri) {
			this.uriToMatch.set(uri);
		}

		public boolean wasControllerUsed() {
			return this.controllerUsed.get();
		}

		public void clear() {
			this.controllerUsed.set(false);
		}
	}
}
