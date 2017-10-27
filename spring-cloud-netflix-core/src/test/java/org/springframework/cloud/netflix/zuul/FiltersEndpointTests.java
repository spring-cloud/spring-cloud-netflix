package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.ZuulFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Tests for Filters endpoint
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FiltersEndpointApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "server.contextPath: /app" })
@DirtiesContext
public class FiltersEndpointTests {

	@Autowired
	private FiltersEndpoint endpoint;

	@Test
	public void getFilters() {
		final Map<String, List<Map<String, Object>>> filters = endpoint.invoke();

		boolean foundFilter = false;

		if (filters.containsKey("sample")) {
			for (Map<String, Object> filterInfo : filters.get("sample")) {
				if (TestFilter.class.getName().equals(filterInfo.get("class"))) {
					foundFilter = true;

					// Verify filter's attributes
					assertEquals(0, filterInfo.get("order"));

					break; // the search is over
				}
			}
		}

		assertTrue(foundFilter, "Could not find expected sample filter from filters endpoint");
	}

}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
class FiltersEndpointApplication {

	@Bean
	public ZuulFilter sampleFilter() {
		return new TestFilter();
	}

}

class TestFilter extends ZuulFilter {
	@Override
	public String filterType() {
		return "sample";
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
}