package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

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
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body, final ClientHttpRequestExecution execution) throws IOException {
        final URI originalUri = request.getURI();
        String serviceName = originalUri.getHost();
        return loadBalancer.execute(serviceName, new LoadBalancerRequest<ClientHttpResponse>() {
            @Override
            public ClientHttpResponse apply(final ServiceInstance instance) throws Exception {
                HttpRequestWrapper wrapper = new HttpRequestWrapper(request) {
                    @Override
                    public URI getURI() {
                        URI uri = loadBalancer.reconstructURI(instance, originalUri);
                        return uri;
                    }
                };
                return execution.execute(wrapper, body);
            }
        });
    }
}
