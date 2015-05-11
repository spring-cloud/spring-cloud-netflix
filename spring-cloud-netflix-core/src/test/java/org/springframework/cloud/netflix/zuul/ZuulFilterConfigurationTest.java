package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.ZuulFilter;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Map;


/**
 * Corresponding unit test class for class {@link ZuulFilterConfigurationTest}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ServletPathZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port: 0", "server.servletPath: /app", "zuul.filter.sendResponse.enabled : false"})
@DirtiesContext
public class ZuulFilterConfigurationTest {

	@Autowired
	private Map<String, ZuulFilter> filters;

	@Test
	public void sendResponseFilterDisabledTest() {
		for (ZuulFilter filter : filters.values()) {
			if (filter instanceof SendResponseFilter) {
				fail();
			}
		}
	}
}
