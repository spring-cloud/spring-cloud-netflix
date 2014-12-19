package org.springframework.cloud.netflix.zuul;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.ZuulFilter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FormZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
	"zuul.routes.simple: /simple/**" })
@DirtiesContext
public class FormZuulProxyApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private ProxyRouteLocator routes;

	@Autowired
	private RoutesEndpoint endpoint;

	@Test
	public void postWithForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + port + "/simple", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String,String>>(form, headers), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted! {foo=[bar]}", result.getBody());
	}

}

//Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClient(name = "simple", configuration = FormRibbonClientConfiguration.class)
class FormZuulProxyApplication {

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public String delete(@RequestBody MultiValueMap<String, String> form) {
		return "Posted! " + form;
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

	public static void main(String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

}

//Load balancer with fixed server list for "simple" pointing to localhost
@Configuration
class FormRibbonClientConfiguration {
	@Bean
	public ILoadBalancer ribbonLoadBalancer(EurekaInstanceConfig instance) {
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("localhost", instance
				.getNonSecurePort())));
		return balancer;
	}
}
