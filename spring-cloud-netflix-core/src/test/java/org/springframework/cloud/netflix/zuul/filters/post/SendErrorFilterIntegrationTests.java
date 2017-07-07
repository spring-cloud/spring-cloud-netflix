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

package org.springframework.cloud.netflix.zuul.filters.post;

import javax.servlet.http.HttpServletRequest;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SendErrorFilterIntegrationTests.Config.class, properties = "zuul.routes.filtertest:/filtertest/**", webEnvironment = RANDOM_PORT)
@DirtiesContext
public class SendErrorFilterIntegrationTests {

	@LocalServerPort
	private int port;

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void testPreFails() {
		String url = "http://localhost:" + port + "/filtertest/get?failpre=true";
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testRouteFails() {
		String url = "http://localhost:" + port + "/filtertest/get?failroute=true";
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testPostFails() {
		String url = "http://localhost:" + port + "/filtertest/get?failpost=true";
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableZuulProxy
	@RestController
	@RibbonClient(name = "filtertest", configuration = RibbonConfig.class)
	protected static class Config {

		@RequestMapping("/get")
		public String get() {
			return "Hello";
		}

		@Bean
		public ZuulFilter testPreFilter() {
			return new FailureFilter() {
				@Override
				public String filterType() {
					return PRE_TYPE;
				}
			};
		}

		@Bean
		public ZuulFilter testRouteFilter() {
			return new FailureFilter() {
				@Override
				public String filterType() {
					return ROUTE_TYPE;
				}
			};
		}

		@Bean
		public ZuulFilter testPostFilter() {
			return new FailureFilter() {
				@Override
				public String filterType() {
					return POST_TYPE;
				}
			};
		}
	}

	public static class RibbonConfig {
		@LocalServerPort
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

	private abstract static class FailureFilter extends ZuulFilter {
		@Override
		public int filterOrder() {
			return Integer.MIN_VALUE;
		}

		@Override
		public boolean shouldFilter() {
			HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
			return request.getParameter("fail" + filterType()) != null;
		}

		@Override
		public Object run() {
			HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
			if (request.getParameter("fail" + filterType()) != null) {
				throw new RuntimeException("failing on purpose in " + filterType());
			}
			return null;
		}
	}
}
