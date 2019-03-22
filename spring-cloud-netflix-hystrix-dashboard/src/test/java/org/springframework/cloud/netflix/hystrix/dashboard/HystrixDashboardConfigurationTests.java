/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.hystrix.dashboard;

import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roy Clarkson
 * @author Fahim Farook
 * @author Biju Kunjummen
 */
public class HystrixDashboardConfigurationTests {

	@Test
	public void normal() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		Header[] headers = new Header[1];
		headers[0] = new BasicHeader("Content-Type", "text/proxy.stream");
		HystrixDashboardConfiguration.ProxyStreamServlet proxyStreamServlet = new HystrixDashboardConfiguration.ProxyStreamServlet();
		ReflectionTestUtils.invokeMethod(proxyStreamServlet,
				"copyHeadersToServletResponse", headers, response);
		assertThat(response.getHeaderNames().size()).isEqualTo(1);
		assertThat(response.getHeader("Content-Type")).isEqualTo("text/proxy.stream");
	}

	@Test
	public void connectionClose() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		Header[] headers = new Header[2];
		headers[0] = new BasicHeader("Content-Type", "text/proxy.stream");
		headers[1] = new BasicHeader("Connection", "close");
		HystrixDashboardConfiguration.ProxyStreamServlet proxyStreamServlet = new HystrixDashboardConfiguration.ProxyStreamServlet();
		ReflectionTestUtils.invokeMethod(proxyStreamServlet,
				"copyHeadersToServletResponse", headers, response);
		assertThat(response.getHeaderNames().size()).isEqualTo(2);
		assertThat(response.getHeader("Content-Type")).isEqualTo("text/proxy.stream");
		assertThat(response.getHeader("Connection")).isEqualTo("close");
	}

	@Test
	public void ignoreConnectionClose() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		Header[] headers = new Header[2];
		headers[0] = new BasicHeader("Content-Type", "text/proxy.stream");
		headers[1] = new BasicHeader("Connection", "close");
		HystrixDashboardConfiguration.ProxyStreamServlet proxyStreamServlet = new HystrixDashboardConfiguration.ProxyStreamServlet();
		proxyStreamServlet.setEnableIgnoreConnectionCloseHeader(true);
		ReflectionTestUtils.invokeMethod(proxyStreamServlet,
				"copyHeadersToServletResponse", headers, response);
		assertThat(response.getHeaderNames().size()).isEqualTo(1);
		assertThat(response.getHeader("Content-Type")).isEqualTo("text/proxy.stream");
		assertThat(response.getHeader("Connection")).isNull();
	}

	@Test
	public void doNotIgnoreConnectionClose() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		Header[] headers = new Header[2];
		headers[0] = new BasicHeader("Content-Type", "text/proxy.stream");
		headers[1] = new BasicHeader("Connection", "close");
		HystrixDashboardConfiguration.ProxyStreamServlet proxyStreamServlet = new HystrixDashboardConfiguration.ProxyStreamServlet();
		proxyStreamServlet.setEnableIgnoreConnectionCloseHeader(false);
		ReflectionTestUtils.invokeMethod(proxyStreamServlet,
				"copyHeadersToServletResponse", headers, response);
		assertThat(response.getHeaderNames().size()).isEqualTo(2);
		assertThat(response.getHeader("Content-Type")).isEqualTo("text/proxy.stream");
		assertThat(response.getHeader("Connection")).isEqualTo("close");
	}

	@Test
	public void initParameters() {
		new ApplicationContextRunner()
				.withUserConfiguration(HystrixDashboardConfiguration.class)
				.withPropertyValues(
						"hystrix.dashboard.init-parameters.wl-dispatch-polixy=work-manager-hystrix")
				.run(context -> {
					final ServletRegistrationBean registration = context
							.getBean(ServletRegistrationBean.class);
					assertThat(registration).isNotNull();

					final Map<String, String> initParameters = registration
							.getInitParameters();
					assertThat(initParameters).isNotNull();
					assertThat(initParameters.get("wl-dispatch-polixy"))
							.isEqualTo("work-manager-hystrix");
				});
	}

}
