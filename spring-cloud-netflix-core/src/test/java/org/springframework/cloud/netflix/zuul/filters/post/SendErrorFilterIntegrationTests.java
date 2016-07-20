package org.springframework.cloud.netflix.zuul.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.support.ZuulProxyTestBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Denys Kurylenko
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SendErrorFilterIntegrationTests.TestConfig.class)
@WebAppConfiguration
@IntegrationTest({"server.port: 0",
                  "zuul.routes.simple: /simple/**"})
@DirtiesContext
public class SendErrorFilterIntegrationTests {

  @Value("${local.server.port}")
  protected int port;

  @Autowired
  RouteLocator routeLocator;

  @Test
  public void responseDataStreamGettingClosed() {
    ResponseEntity<String> result = new TestRestTemplate().exchange(
        "http://localhost:" + this.port + "/simple",
        HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
    assertEquals("true", result.getHeaders().getFirst("x-response-stream-closed"));
  }

  // Don't use @SpringBootApplication because we don't want to component scan
  @Configuration
  @EnableAutoConfiguration
  @RestController
  @EnableZuulProxy
  @RibbonClients({
      @RibbonClient(name = "simple", configuration = ZuulProxyTestBase.SimpleRibbonClientConfiguration.class) })
  static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication
  {
    @Bean
    public ZuulFilter bogusPostFilter(final SendErrorFilter sendErrorFilter)
    {
      return new ZuulFilter()
      {
        @Override
        public boolean shouldFilter()
        {
          return true;
        }

        @Override
        public Object run()
        {
          final RequestContext ctx = RequestContext.getCurrentContext();
          final InputStream responseDataStream = ctx.getResponseDataStream();
          if (responseDataStream != null)
          {
            ctx.setResponseDataStream(new CloseAwareInputStream(responseDataStream));
          }
          throw new IllegalStateException("Internal exception");
        }

        @Override
        public String filterType()
        {
          return "post";
        }

        @Override
        public int filterOrder()
        {
          return sendErrorFilter.filterOrder() - 10;
        }
      };
    }
  }

  static class CloseAwareInputStream extends ProxyInputStream
  {
    public CloseAwareInputStream(InputStream proxy)
    {
      super(proxy);
    }

    @Override
    public void close() throws IOException
    {
      RequestContext.getCurrentContext().getResponse().setHeader("x-response-stream-closed", "true");
      super.close();
    }
  }
}
