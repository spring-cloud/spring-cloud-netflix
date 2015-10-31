package org.springframework.cloud.netflix.zuul.filters;

import com.netflix.zuul.context.RequestContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.RoutesEndpoint;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfiguration;
import org.springframework.cloud.netflix.zuul.filters.route.HostRoutingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleCustomZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0", "server.contextPath: /app" })
@DirtiesContext
public class HostRoutingFilterTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ProxyRouteLocator routes;

    @Autowired
    private RoutesEndpoint endpoint;

    @Test
    public void getOnSelfViaCustomHostRoutingFilter() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app");
        this.endpoint.reset();
        ResponseEntity<String> result = new TestRestTemplate().getForEntity(
                "http://localhost:" + this.port + "/app/self/get/1", String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Get 1", result.getBody());
    }

    @Test
    public void postOnSelfViaCustomHostRoutingFilter() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app");
        this.endpoint.reset();
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("id", "2");
        ResponseEntity<String> result = new TestRestTemplate().postForEntity(
                "http://localhost:" + this.port + "/app/self/post", params, String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Post 2", result.getBody());
    }

    @Test
    public void putOnSelfViaCustomHostRoutingFilter() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app");
        this.endpoint.reset();
        ResponseEntity<String> result = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/app/self/put/3", HttpMethod.PUT,
                new HttpEntity<>((Void) null), String.class);
                assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Put 3", result.getBody());
    }

    @Test
    public void patchOnSelfViaCustomHostRoutingFilter() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app");
        this.endpoint.reset();
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("patch", "5");
        ResponseEntity<String> result = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/app/self/patch/4", HttpMethod.PATCH,
                new HttpEntity<>(params), String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Patch 45", result.getBody());
    }
    @Test
    public void getOnSelfIgnoredHeaders() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app");
        this.endpoint.reset();
        ResponseEntity<String> result = new TestRestTemplate().getForEntity(
                "http://localhost:" + this.port + "/app/self/get/1", String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getHeaders().containsKey("X-NotIgnored"));
        assertFalse(result.getHeaders().containsKey("X-Ignored"));
    }

}

@Configuration
@EnableAutoConfiguration
@RestController
class SampleCustomZuulProxyApplication {

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    public String get(@PathVariable String id, HttpServletResponse response) {
        response.setHeader("X-Ignored", "foo");
        response.setHeader("X-NotIgnored", "bar");
        return "Get " + id;
    }

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public String post(@RequestParam("id") String id) {
        return "Post " + id;
    }

    @RequestMapping(value = "/put/{id}", method = RequestMethod.PUT)
    public String put(@PathVariable String id) {
        return "Put " + id;
    }

    @RequestMapping(value = "/patch/{id}", method = RequestMethod.PATCH)
    public String patch(@PathVariable String id, @RequestParam("patch") String patch) {
        return "Patch " + id + patch;
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleCustomZuulProxyApplication.class, args);
    }

    @Configuration
    @EnableZuulProxy
    protected static class CustomZuulProxyConfig extends ZuulProxyConfiguration {
        @Bean
        @Override
        public HostRoutingFilter hostRoutingFilter() {
            ProxyRequestHelper helper = new ProxyRequestHelper();
            helper.addIgnoredHeaders("X-Ignored");
            return new CustomHostRoutingFilter(helper);
        }

        private class CustomHostRoutingFilter extends HostRoutingFilter {
            private HttpClient httpClient;

            public CustomHostRoutingFilter(ProxyRequestHelper helper) {
                super(helper);
            }

            @Override
            protected HttpResponse forward(String verb, String uri, HttpServletRequest request, MultiValueMap<String, String> headers,
                                           MultiValueMap<String, String> params, InputStream requestEntity) throws Exception {
                URL host = RequestContext.getCurrentContext().getRouteHost();
                HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
                uri = StringUtils.cleanPath(host.getPath() + uri);
                HttpRequest httpRequest;
                switch (verb.toUpperCase()) {
                    case "POST":
                        HttpPost httpPost = new HttpPost(uri + getQueryString());
                        httpRequest = httpPost;
                        httpPost.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
                        break;
                    case "PUT":
                        HttpPut httpPut = new HttpPut(uri + getQueryString());
                        httpRequest = httpPut;
                        httpPut.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
                        break;
                    case "PATCH":
                        HttpPatch httpPatch = new HttpPatch(uri + getQueryString());
                        httpRequest = httpPatch;
                        httpPatch.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
                        break;
                    default:
                        httpRequest = new BasicHttpRequest(verb, uri + getQueryString());
                }
                httpRequest.setHeaders(convertHeaders(headers));
                return getHttpClient().execute(httpHost, httpRequest);
            }

            private HttpClient getHttpClient() throws Exception {
                if (httpClient == null) {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }
                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }}, new SecureRandom());

                    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .register("https", new SSLConnectionSocketFactory(sslContext))
                            .build();

                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
                    connectionManager.setMaxTotal(Integer.parseInt(System.getProperty("zuul.max.host.connections", "200")));
                    connectionManager.setDefaultMaxPerRoute(Integer.parseInt(System.getProperty("zuul.max.host.connections", "20")));

                    RequestConfig requestConfig = RequestConfig.custom()
                            .setSocketTimeout(10000)
                            .setConnectTimeout(2000)
                            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                            .build();

                    httpClient = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .setDefaultRequestConfig(requestConfig)
                            .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                            .setRedirectStrategy(new RedirectStrategy() {
                                @Override
                                public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                                    return false;
                                }

                                @Override
                                public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                                    return null;
                                }
                            })
                            .build();
                }
                return httpClient;
            }

            private String getQueryString() throws UnsupportedEncodingException {
                HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
                MultiValueMap<String, String> params = helper.buildZuulRequestQueryParams(request);
                StringBuilder query = new StringBuilder();
                for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                    String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                    for (String value : entry.getValue()) {
                        query.append("&");
                        query.append(key);
                        query.append("=");
                        query.append(URLEncoder.encode(value, "UTF-8"));
                    }
                }
                return (query.length() > 0) ? "?" + query.substring(1) : "";
            }

        }
    }

}
