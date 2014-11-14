package org.springframework.cloud.netflix.ribbon;

import com.google.common.base.Throwables;
import com.netflix.loadbalancer.Server;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Spencer Gibb
 */
public class RibbonInterceptorTest {

    @Mock
    HttpRequest request;

    @Mock
    ClientHttpRequestExecution execution;

    @Mock
    ClientHttpResponse response;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIntercept() throws Exception {
        RibbonServer server = new RibbonServer("myservice", new Server("myhost", 8080));
        RibbonInterceptor interceptor = new RibbonInterceptor(new MyClient(server));

        when(request.getURI()).thenReturn(new URL("http://myservice").toURI());
        when(execution.execute(isA(HttpRequest.class), isA(byte[].class))).thenReturn(response);
        ArgumentCaptor<HttpRequestWrapper> argument = ArgumentCaptor.forClass(HttpRequestWrapper.class);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertNotNull("response was null", response);
        verify(execution).execute(argument.capture(), isA(byte[].class));
        HttpRequestWrapper wrapper = argument.getValue();
        assertEquals("wrong constructed uri", new URL("http://myhost:8080").toURI(), wrapper.getURI());
    }

    protected static class MyClient implements LoadBalancerClient {
        ServiceInstance instance;

        public MyClient(ServiceInstance instance) {
            this.instance = instance;
        }

        @Override
        public ServiceInstance choose(String serviceId) {
            return instance;
        }

        @Override
        public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
            try {
                return request.apply(instance);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            return null;
        }

        @Override
        public URI reconstructURI(ServiceInstance instance, URI original) {
            return UriComponentsBuilder.fromUri(original)
                    .host(instance.getHost())
                    .port(instance.getPort())
                    .build()
                    .toUri();
        }
    }
}
