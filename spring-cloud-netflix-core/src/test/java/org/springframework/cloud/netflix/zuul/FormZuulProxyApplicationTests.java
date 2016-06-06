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
import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FormZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port:0", "zuul.routes.simple:/simple/**" })
@DirtiesContext
public class FormZuulProxyApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@Test
	public void postWithForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/form", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithMultipartForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/form", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithMultipartFile() {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
		HttpHeaders part = new HttpHeaders();
		part.setContentType(MediaType.TEXT_PLAIN);
		part.setContentDispositionFormData("file", "foo.txt");
		form.set("foo", new HttpEntity<byte[]>("bar".getBytes(), part));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/file", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, Object>>(form, headers),
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! bar", result.getBody());
	}

	@Test
	public void postWithUTF8Form() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(
				MediaType.APPLICATION_FORM_URLENCODED_VALUE + "; charset=UTF-8"));
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/form", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

	@Test
	public void postWithUrlParams() throws Exception {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(
				MediaType.APPLICATION_FORM_URLENCODED_VALUE + "; charset=UTF-8"));
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/form?uriParam=uriValue",
				HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {uriParam=[uriValue], foo=[bar]}", result.getBody());
	}

	@Test
	public void getWithUrlParams() throws Exception {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/form?uriParam=uriValue",
				HttpMethod.GET, null, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {uriParam=[uriValue]}", result.getBody());
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
@Slf4j
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
		byte[] bytes = new byte[0];
		if (file != null) {
			if (file.getSize() > 1024) {
				bytes = new byte[1024];
				InputStream inputStream = file.getInputStream();
				inputStream.read(bytes);
				byte[] buffer = new byte[1024 * 1024 * 10];
				while (inputStream.read(buffer) >= 0) {
					log.info("Read more bytes");
				}
			}
			else {
				bytes = file.getBytes();
			}
		}
		return "Posted! " + new String(bytes);
	}

	@Bean
	public ZuulFilter sampleFilter() {
		return new ZuulFilter() {

			@Override
			public String filterType() {
				return "pre";
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

	@Value("${local.server.port}")
	private int port;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}

}
