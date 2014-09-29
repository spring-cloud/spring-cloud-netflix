package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.ClientFactory;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
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
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                URI originalUri = super.getURI();
                String serviceName = originalUri.getHost();
                ILoadBalancer loadBalancer = ClientFactory.getNamedLoadBalancer(serviceName);
                Server server = loadBalancer.chooseServer(null);
                if (server == null) {
                    throw new IllegalStateException("Unable to locate ILoadBalancer for service: "+ serviceName);
                }
                URI uri = UriComponentsBuilder.fromUri(originalUri)
                        .host(server.getHost())
                        .port(server.getPort())
                        .build()
                        .toUri();
                return uri;
            }
        };
        return execution.execute(wrapper, body);
    }
}
