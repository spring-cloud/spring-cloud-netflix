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

package org.springframework.cloud.netflix.eureka.server;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.server.ApplicationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.wrappers.CodecWrapper;
import com.netflix.eureka.resources.ServerCodecs;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.jmx.enabled=true", "management.security.enabled=false" })
public class ApplicationTests {

	@LocalServerPort
	private int port = 0;

	@Autowired
	private ServerCodecs serverCodecs;

	@Test
	public void catalogLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/eureka/apps", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void adminLoads() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/env", HttpMethod.GET,
				new HttpEntity<>("parameters", headers), Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void noDoubleSlashes() {
		String basePath = "http://localhost:" + this.port + "/";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(basePath,
				String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull(body);
		assertFalse("basePath contains double slashes", body.contains(basePath + "/"));
	}

	@Test
	public void cssParsedByLess() {
		String basePath = "http://localhost:" + this.port + "/eureka/css/wro.css";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(basePath,
				String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull(body);
		assertTrue("css wasn't preprocessed", body.contains("spring-logo"));
	}

	@Test
	public void customCodecWorks() throws Exception {
		assertThat("serverCodecs is wrong type", this.serverCodecs,
				is(instanceOf(EurekaServerAutoConfiguration.CloudServerCodecs.class)));
		CodecWrapper codec = this.serverCodecs.getFullJsonCodec();
		assertThat("codec is wrong type", codec, is(instanceOf(CloudJacksonJson.class)));

		InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("fooapp")
				.add("instanceId", "foo").build();
		String encoded = codec.encode(instanceInfo);
		InstanceInfo decoded = codec.decode(encoded, InstanceInfo.class);
		assertThat("instanceId was wrong", decoded.getInstanceId(), is("foo"));
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {
		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=eureka").run(args);
		}
	}

}
