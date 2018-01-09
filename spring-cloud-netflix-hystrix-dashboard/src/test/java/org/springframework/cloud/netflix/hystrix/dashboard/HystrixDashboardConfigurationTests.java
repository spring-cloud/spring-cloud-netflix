/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.dashboard;

import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
		assertThat(response.getHeaderNames().size(), is(1));
		assertThat(response.getHeader("Content-Type"), is("text/proxy.stream"));
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
		assertThat(response.getHeaderNames().size(), is(2));
		assertThat(response.getHeader("Content-Type"), is("text/proxy.stream"));
		assertThat(response.getHeader("Connection"), is("close"));
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
		assertThat(response.getHeaderNames().size(), is(1));
		assertThat(response.getHeader("Content-Type"), is("text/proxy.stream"));
		assertNull(response.getHeader("Connection"));
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
		assertThat(response.getHeaderNames().size(), is(2));
		assertThat(response.getHeader("Content-Type"), is("text/proxy.stream"));
		assertThat(response.getHeader("Connection"), is("close"));
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
				assertNotNull(registration);

				final Map<String, String> initParameters = registration
					.getInitParameters();
				assertNotNull(initParameters);
				assertThat(initParameters.get("wl-dispatch-polixy"),
					is("work-manager-hystrix"));
			});
	}
}