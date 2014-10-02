package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * @author Spencer Gibb
 */
public class RibbonInterceptor implements ClientHttpRequestInterceptor {

    private LoadBalancerClient loadBalancer;

    public RibbonInterceptor(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                URI originalUri = super.getURI();
                String serviceName = originalUri.getHost();
                ServiceInstance instance = loadBalancer.choose(serviceName);
                URI uri = UriComponentsBuilder.fromUri(originalUri)
                        .host(instance.getHost())
                        .port(instance.getPort())
                        .build()
                        .toUri();
                return uri;
            }
        };
        return execution.execute(wrapper, body);
    }
}
