package org.springframework.cloud.netflix.feign.ribbon;

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.google.common.base.Throwables;
import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import dagger.Lazy;
import feign.Client;
import feign.Request;
import feign.Response;

/**
 * @author Julien Roy
 * @author Spencer Gibb
 */
public class FeignRibbonClient implements Client {

    private Client defaultClient = new Default(
        new Lazy<SSLSocketFactory>() {
            @Override
            public SSLSocketFactory get() {
                return (SSLSocketFactory) SSLSocketFactory.getDefault();
            }
        }, new Lazy<HostnameVerifier>() {
            @Override
            public HostnameVerifier get() {
                return HttpsURLConnection.getDefaultHostnameVerifier();
            }
    });

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        try {

            URI asUri = URI.create(request.url());
            String clientName = asUri.getHost();
            URI uriWithoutSchemeAndPort = URI.create(request.url().replace(asUri.getScheme() + "://" + asUri.getHost(), ""));
            RibbonLoadBalancer.RibbonRequest ribbonRequest = new RibbonLoadBalancer.RibbonRequest(request, uriWithoutSchemeAndPort);
            return lbClient(clientName).executeWithLoadBalancer(ribbonRequest).toResponse();

        } catch (ClientException e) {
            if (e.getCause() instanceof IOException) {
                throw IOException.class.cast(e.getCause());
            }
            throw Throwables.propagate(e);
        }
    }

    private RibbonLoadBalancer lbClient(String clientName) {
        IClientConfig config = ClientFactory.getNamedConfig(clientName);
        ILoadBalancer lb = ClientFactory.getNamedLoadBalancer(clientName);
        return new RibbonLoadBalancer(defaultClient, lb, config);
    }

    public void setDefaultClient(Client defaultClient) {
        this.defaultClient = defaultClient;
    }
}
