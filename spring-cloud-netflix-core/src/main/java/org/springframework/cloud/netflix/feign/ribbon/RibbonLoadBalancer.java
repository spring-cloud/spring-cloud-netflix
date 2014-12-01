package org.springframework.cloud.netflix.feign.ribbon;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;

public class RibbonLoadBalancer extends AbstractLoadBalancerAwareClient<RibbonLoadBalancer.RibbonRequest, RibbonLoadBalancer.RibbonResponse> {

        private final Client delegate;
        private final int connectTimeout;
        private final int readTimeout;
        private final IClientConfig clientConfig;

        public RibbonLoadBalancer(Client delegate, ILoadBalancer lb, IClientConfig clientConfig) {
            super(lb, clientConfig);
            this.setRetryHandler(RetryHandler.DEFAULT);
            this.clientConfig = clientConfig;
            this.delegate = delegate;
            connectTimeout = clientConfig.get(CommonClientConfigKey.ConnectTimeout);
            readTimeout = clientConfig.get(CommonClientConfigKey.ReadTimeout);
        }

        @Override
        public RibbonResponse execute(RibbonRequest request, IClientConfig configOverride) throws IOException {
            Request.Options options;
            if (configOverride != null) {
                options = new Request.Options(configOverride.get(CommonClientConfigKey.ConnectTimeout, connectTimeout), (configOverride.get(CommonClientConfigKey.ReadTimeout, readTimeout)));
            } else {
                options = new Request.Options(connectTimeout, readTimeout);
            }
            Response response = delegate.execute(request.toRequest(), options);
            return new RibbonResponse(request.getUri(), response);
        }

        @Override
        public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
                RibbonRequest request, IClientConfig requestConfig) {
            if (clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations, false)) {
                return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
            }
            if (!request.toRequest().method().equals("GET")) {
                return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(), requestConfig);
            } else {
                return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
            }
        }

        static class RibbonRequest extends ClientRequest implements Cloneable {

            private final Request request;

            RibbonRequest(Request request, URI uri) {
                this.request = request;
                setUri(uri);
            }

            Request toRequest() {
                return new RequestTemplate()
                        .method(request.method())
                        .append(getUri().toASCIIString())
                        .headers(request.headers())
                        .body(request.body(), request.charset())
                        .request();
            }

            public Object clone() {
                return new RibbonRequest(request, getUri());
            }
        }

        static class RibbonResponse implements IResponse {

            private final URI uri;
            private final Response response;

            RibbonResponse(URI uri, Response response) {
                this.uri = uri;
                this.response = response;
            }

            @Override
            public Object getPayload() throws ClientException {
                return response.body();
            }

            @Override
            public boolean hasPayload() {
                return response.body() != null;
            }

            @Override
            public boolean isSuccess() {
                return response.status() == 200;
            }

            @Override
            public URI getRequestedURI() {
                return uri;
            }

            @Override
            public Map<String, Collection<String>> getHeaders() {
                return response.headers();
            }

            Response toResponse() {
                return response;
            }

            @Override
            public void close() throws IOException {
                if (response != null && response.body() != null) {
                    response.body().close();
                }
            }

        }
    }
