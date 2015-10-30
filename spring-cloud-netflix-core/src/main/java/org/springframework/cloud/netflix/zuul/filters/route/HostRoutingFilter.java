package org.springframework.cloud.netflix.zuul.filters.route;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public abstract class HostRoutingFilter extends ZuulFilter {

    protected static final DynamicIntProperty SOCKET_TIMEOUT = DynamicPropertyFactory
            .getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_SOCKET_TIMEOUT_MILLIS,
                    10000);

    protected static final DynamicIntProperty CONNECTION_TIMEOUT = DynamicPropertyFactory
            .getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_CONNECT_TIMEOUT_MILLIS,
                    2000);

    protected static final Timer CONNECTION_MANAGER_TIMER = new Timer(
            "SimpleHostRoutingFilter.CONNECTION_MANAGER_TIMER", true);

    protected ProxyRequestHelper helper;

    public HostRoutingFilter() {
        this(new ProxyRequestHelper());
    }

    public HostRoutingFilter(ProxyRequestHelper helper) {
        this.helper = helper;
    }

    @Override
    public String filterType() {
        return "route";
    }

    @Override
    public int filterOrder() {
        return 100;
    }

    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getRouteHost() != null
                && RequestContext.getCurrentContext().sendZuulResponse();
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
        String verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);
        String uri = this.helper.buildZuulRequestURI(request);
        try {
            HttpResponse response = forward(verb, uri, request, headers, params, requestEntity);
            setResponse(response);
        } catch (Exception ex) {
            context.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            context.set("error.exception", ex);
        }
        return null;
    }

    protected String getVerb(HttpServletRequest request) {
        return request.getMethod().toUpperCase();
    }

    protected InputStream getRequestBody(HttpServletRequest request) {
        InputStream requestEntity = null;
        try {
            requestEntity = request.getInputStream();
        } catch (IOException ex) {
            // no requestBody is ok.
        }
        return requestEntity;
    }

    protected void setResponse(HttpResponse response) throws IOException {
        this.helper.setResponse(response.getStatusLine().getStatusCode(),
                response.getEntity() == null ? null : response.getEntity().getContent(),
                revertHeaders(response.getAllHeaders()));
    }

    protected MultiValueMap<String, String> revertHeaders(Header[] headers) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        for (Header header : headers) {
            String name = header.getName();
            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<String>());
            }
            map.get(name).add(header.getValue());
        }
        return map;
    }

    protected Header[] convertHeaders(MultiValueMap<String, String> headers) {
        List<Header> list = new ArrayList<>();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                list.add(new BasicHeader(name, value));
            }
        }
        return list.toArray(new Header[list.size()]);
    }

    protected abstract HttpResponse forward(String verb, String uri,
                                            HttpServletRequest request, MultiValueMap<String, String> headers,
                                            MultiValueMap<String, String> params, InputStream requestEntity) throws Exception;

}
