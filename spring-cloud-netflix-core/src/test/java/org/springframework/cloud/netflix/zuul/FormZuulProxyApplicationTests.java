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

package org.springframework.cloud.netflix.zuul;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.Part;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.Assert.assertEquals;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.util.StreamUtils.copyToString;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FormZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.routes.simple:/simple/**" })
@DirtiesContext
public class FormZuulProxyApplicationTests {

	@Inject
	private TestRestTemplate restTemplate;

	@Before
	public void setTestRequestContext() {
		RequestContext.testSetCurrentContext(new RequestContext());
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void postWithForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity result = sendPost("/simple/form", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithMultipartForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity result = sendPost("/simple/form", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithMultipartFile() {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

		HttpHeaders part = new HttpHeaders();
		part.setContentType(MediaType.TEXT_PLAIN);
		part.setContentDispositionFormData("file", "foo.txt");

		form.set("foo", new HttpEntity<>("bar".getBytes(), part));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity result = sendPost("/simple/file", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! bar", result.getBody());
	}

	@Test
	public void postWithMultipartFileAndForm() {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

		HttpHeaders part = new HttpHeaders();
		part.setContentType(MediaType.TEXT_PLAIN);
		part.setContentDispositionFormData("file", "foo.txt");
		form.set("foo", new HttpEntity<>("bar".getBytes(), part));

		form.set("field", "data");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity result = sendPost("/simple/fileandform", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! bar!field!data", result.getBody());
	}

	@Test
	public void postWithMultipartApplicationJson() {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

		HttpHeaders partHeaders = new HttpHeaders();
		partHeaders.setContentType(MediaType.APPLICATION_JSON);
		form.set("field", new HttpEntity<>("{foo=[bar]}", partHeaders));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity result = sendPost("/simple/json", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]} as application/json", result.getBody());
	}

	@Test
	public void postWithUTF8Form() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

		form.set("foo", "bar");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(
				MediaType.APPLICATION_FORM_URLENCODED_VALUE + "; charset=UTF-8"));

		ResponseEntity result = sendPost("/simple/form", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithUrlParams() throws Exception {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

		form.set("foo", "bar");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(
				MediaType.APPLICATION_FORM_URLENCODED_VALUE + "; charset=UTF-8"));

		ResponseEntity result = sendPost("/simple/form?uriParam=uriValue", form, headers);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {uriParam=[uriValue], foo=[bar]}", result.getBody());
	}

	@Test
	public void getWithUrlParams() throws Exception {
		ResponseEntity<String> result = sendGet("/simple/form?uriParam=uriValue");

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {uriParam=[uriValue]}", result.getBody());
	}

	private ResponseEntity<String> sendPost(String url, MultiValueMap form,
			HttpHeaders headers) {
		return restTemplate.postForEntity(url, new HttpEntity<>(form, headers),
				String.class);
	}

	private ResponseEntity<String> sendGet(String url) {
		return restTemplate.getForEntity(url, String.class);
	}
}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClients({
		@RibbonClient(name = "simple", configuration = FormRibbonClientConfiguration.class),
		@RibbonClient(name = "psimple", configuration = FormRibbonClientConfiguration.class) })
class FormZuulProxyApplication {

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String accept(@RequestParam MultiValueMap<String, String> form)
			throws IOException {
		return "Posted! " + form;
	}

	@RequestMapping(value = "/form", method = RequestMethod.GET)
	public String get(@RequestParam MultiValueMap<String, String> form)
			throws IOException {
		return "Posted! " + form;
	}

	// TODO: Why does this not work if you add @RequestParam as above?
	@RequestMapping(value = "/file", method = RequestMethod.POST)
	public String file(@RequestParam(required = false) MultipartFile file)
			throws IOException {

		return "Posted! " + copyToString(file.getInputStream(), defaultCharset());
	}

	@RequestMapping(value = "/fileandform", method = RequestMethod.POST)
	public String fileAndForm(@RequestParam MultipartFile file,
			@RequestParam String field) throws IOException {

		return "Posted! " + copyToString(file.getInputStream(), defaultCharset())
				+ "!field!" + field;
	}

	@RequestMapping(value = "/json", method = RequestMethod.POST)
	public String fileAndJson(@RequestPart Part field) throws IOException {

		return "Posted! " + copyToString(field.getInputStream(), defaultCharset())
				+ " as " + field.getContentType();
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
				return null;
			}

			@Override
			public int filterOrder() {
				return 0;
			}

		};
	}

	@Bean
	public TraceRepository traceRepository() {
		return new InMemoryTraceRepository() {
			@Override
			public void add(Map<String, Object> map) {
				if (map.containsKey("body")) {
					map.get("body");
				}
				super.add(map);
			}
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(FormZuulProxyApplication.class)
				.properties("zuul.routes.simple:/simple/**",
						"zuul.routes.direct.url:http://localhost:9999",
						"multipart.maxFileSize:4096MB", "multipart.maxRequestSize:4096MB")
				.run(args);
	}

}

// Load balancer with fixed server list for "simple" pointing to localhost
@Configuration
class FormRibbonClientConfiguration {

	@LocalServerPort
	private int port;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}

}
