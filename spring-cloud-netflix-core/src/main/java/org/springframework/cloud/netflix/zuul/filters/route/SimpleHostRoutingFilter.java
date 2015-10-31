/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.http.*;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

@CommonsLog
public class SimpleHostRoutingFilter extends HostRoutingFilter {

    private static final DynamicIntProperty SOCKET_TIMEOUT = DynamicPropertyFactory
            .getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_SOCKET_TIMEOUT_MILLIS,
                    10000);

    private static final DynamicIntProperty CONNECTION_TIMEOUT = DynamicPropertyFactory
            .getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_CONNECT_TIMEOUT_MILLIS,
                    2000);

    private static final Timer CONNECTION_MANAGER_TIMER = new Timer(
            "SimpleHostRoutingFilter.CONNECTION_MANAGER_TIMER", true);

    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;

    public SimpleHostRoutingFilter() {
        super();
    }

    public SimpleHostRoutingFilter(ProxyRequestHelper helper) {
        super(helper);
    }

    @PostConstruct
    private void initialize() {
        this.httpClient = newClient();

        final Runnable CLIENTLOADER = new Runnable() {
            @Override
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException ex) {
                    log.error("error closing client", ex);
                } finally {
                    httpClient = newClient();
                }
            }
        };

        SOCKET_TIMEOUT.addCallback(CLIENTLOADER);
        CONNECTION_TIMEOUT.addCallback(CLIENTLOADER);
        CONNECTION_MANAGER_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if (connectionManager == null) {
                    return;
                }
                connectionManager.closeExpiredConnections();
            }
        }, 30000, 5000);
    }

    @PreDestroy
    public void stop() {
        CONNECTION_MANAGER_TIMER.purge();
    }

    @Override
    protected HttpResponse forward(String verb, String uri,
                                   HttpServletRequest request, MultiValueMap<String, String> headers,
                                   MultiValueMap<String, String> params, InputStream requestEntity)
            throws Exception {
        Map<String, Object> info = this.helper.debug(verb, uri, headers, params,
                requestEntity);
        URL host = RequestContext.getCurrentContext().getRouteHost();
        HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(),
                host.getProtocol());
        uri = StringUtils.cleanPath(host.getPath() + uri);
        HttpRequest httpRequest;
        switch (verb.toUpperCase()) {
            case "POST":
                HttpPost httpPost = new HttpPost(uri + getQueryString());
                httpRequest = httpPost;
                httpPost.setEntity(new InputStreamEntity(requestEntity, request
                        .getContentLength()));
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uri + getQueryString());
                httpRequest = httpPut;
                httpPut.setEntity(new InputStreamEntity(requestEntity, request
                        .getContentLength()));
                break;
            case "PATCH":
                HttpPatch httpPatch = new HttpPatch(uri + getQueryString());
                httpRequest = httpPatch;
                httpPatch.setEntity(new InputStreamEntity(requestEntity, request
                        .getContentLength()));
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uri + getQueryString());
                log.debug(uri + getQueryString());
        }
        httpRequest.setHeaders(convertHeaders(headers));
        log.debug(httpHost.getHostName() + " " + httpHost.getPort() + " "
                + httpHost.getSchemeName());
        HttpResponse zuulResponse = httpClient.execute(httpHost, httpRequest);
        this.helper.appendDebug(info, zuulResponse.getStatusLine().getStatusCode(),
                revertHeaders(zuulResponse.getAllHeaders()));
        return zuulResponse;
    }

    protected PoolingHttpClientConnectionManager newConnectionManager() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
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

            final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(sslContext))
                    .build();

            connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(Integer.parseInt(System.getProperty("zuul.max.host.connections", "200")));
            connectionManager.setDefaultMaxPerRoute(Integer.parseInt(System.getProperty("zuul.max.host.connections", "20")));
            return connectionManager;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected CloseableHttpClient newClient() {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SOCKET_TIMEOUT.get())
                .setConnectTimeout(CONNECTION_TIMEOUT.get())
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();

        return HttpClients.custom()
                .setConnectionManager(newConnectionManager())
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

    private String getQueryString() throws UnsupportedEncodingException {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        MultiValueMap<String, String> params=helper.buildZuulRequestQueryParams(request);
        StringBuilder query=new StringBuilder();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String key=URLEncoder.encode(entry.getKey(), "UTF-8");
            for (String value : entry.getValue()) {
                query.append("&");
                query.append(key);
                query.append("=");
                query.append(URLEncoder.encode(value, "UTF-8"));
            }
        }
        return (query.length()>0) ? "?" + query.substring(1) : "";
    }

    private Header[] convertHeaders(MultiValueMap<String, String> headers) {
        List<Header> list = new ArrayList<>();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                list.add(new BasicHeader(name, value));
            }
        }
        return list.toArray(new Header[list.size()]);
    }

}
