package org.springframework.platform.netflix.zuul.filters.route;

import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.util.HTTPRequestUtils;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.platform.netflix.zuul.RibbonCommand;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static com.netflix.client.http.HttpRequest.Verb;
import static org.springframework.platform.netflix.feign.FeignConfigurer.setServiceListClassAndVIP;

public class RibbonRoutingFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RibbonRoutingFilter.class);

    public static final String CONTENT_ENCODING = "Content-Encoding";

    @Override
    public String filterType() {
        return "route";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        return (ctx.getRouteHost() == null && ctx.get("serviceId") != null && ctx.sendZuulResponse());
    }

    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        MultivaluedMap<String, String> headers = buildZuulRequestHeaders(request);
        MultivaluedMap<String, String> params = buildZuulRequestQueryParams(request);
        Verb verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);

        String serviceId = (String) context.get("serviceId");

        //TODO: can this be set be default? or an implementation of an interface?
        setServiceListClassAndVIP(serviceId);

        RestClient restClient = (RestClient) ClientFactory.getNamedClient(serviceId);

        String uri = request.getRequestURI();
        if (context.get("requestURI") != null) {
            uri = (String) context.get("requestURI");
        }
        //remove double slashes
        uri = uri.replace("//", "/");

        try {
            HttpResponse response = forward(restClient, verb, uri, headers, params, requestEntity);
            setResponse(response);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void debug(RestClient restClient, Verb verb, String uri, MultivaluedMap<String, String> headers,
               MultivaluedMap<String, String> params, InputStream requestEntity) throws IOException {

        if (Debug.debugRequest()) {

            for (String header: headers.keySet()) {
                Debug.addRequestDebug(String.format("ZUUL:: > %s  %s", header, headers.getFirst(header)));
            }
            StringBuilder query = new StringBuilder();
            for (String param : params.keySet()) {
                for (String value : params.get(param)) {
                    query.append(param);
                    query.append("=");
                    query.append(value);
                    query.append("&");
                }
            }

            Debug.addRequestDebug(String.format("ZUUL:: > %s  %s?%s HTTP/1.1",  verb.verb(), uri, query.toString()));
            RequestContext ctx = RequestContext.getCurrentContext();
            if (!ctx.isChunkedRequestBody()) {
                if (requestEntity != null) {
                    debugRequestEntity(ctx.getRequest().getInputStream());
                }
            }
        }
    }

    private void debugRequestEntity(InputStream inputStream) throws IOException {
        if (!Debug.debugRequestHeadersOnly()) {
            String entity = IOUtils.toString(inputStream);
            Debug.addRequestDebug("ZUUL:: > "+entity);
        }
    }



    private HttpResponse forward(RestClient restClient, Verb verb, String uri, MultivaluedMap<String, String> headers,
                             MultivaluedMap<String, String> params, InputStream requestEntity) throws Exception {
        debug(restClient, verb, uri, headers, params, requestEntity);

        RibbonCommand command = new RibbonCommand(restClient, verb, uri, headers, params, requestEntity);
        try {
            HttpResponse response = command.execute();
            return response;
        } catch (HystrixRuntimeException e) {
            if (e.getFallbackException() != null &&
                e.getFallbackException().getCause() != null &&
                e.getFallbackException().getCause()  instanceof ClientException) {
                ClientException ex = (ClientException) e.getFallbackException().getCause();
                throw new ZuulException(ex, "Forwarding error", 500, ex.getErrorType().toString());
            }
            throw new ZuulException(e, "Forwarding error", 500, e.getFailureType().toString());
        }

    }


    private InputStream getRequestBody(HttpServletRequest request) {
        InputStream requestEntity = null;
        try {
            requestEntity = (InputStream) RequestContext.getCurrentContext().get("requestEntity");
            if (requestEntity == null) {
                requestEntity = request.getInputStream();
            }
        } catch (IOException e) {
            LOG.error("Error during getRequestBody", e);
        }

        return requestEntity;
    }

    private MultivaluedMap<String, String> buildZuulRequestQueryParams(HttpServletRequest request) {

        Map<String, List<String>> map = HTTPRequestUtils.getInstance().getQueryParams();

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        if (map == null) return params;

        for (String key : map.keySet()) {

            for (String value : map.get(key)) {
                params.add(key, value);
            }
        }
        return params;
    }

    private MultivaluedMap<String, String> buildZuulRequestHeaders(HttpServletRequest request) {

        RequestContext context = RequestContext.getCurrentContext();

        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        Enumeration headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = (String) headerNames.nextElement();
                String value = request.getHeader(name);
                if (!name.toLowerCase().contains("content-length")) headers.putSingle(name, value);
            }
        }
        Map<String, String> zuulRequestHeaders = context.getZuulRequestHeaders();

        for (String header : zuulRequestHeaders.keySet()) {
            headers.putSingle(header, zuulRequestHeaders.get(header));
        }

        headers.putSingle("accept-encoding", "deflate, gzip");

        if (headers.containsKey("transfer-encoding"))
            headers.remove("transfer-encoding");

        return headers;
    }



    Verb getVerb(HttpServletRequest request) {
        String sMethod = request.getMethod();
        return getVerb(sMethod);
    }

    Verb getVerb(String sMethod) {
        if (sMethod == null) return Verb.GET;
        sMethod = sMethod.toLowerCase();
        if (sMethod.equals("post")) return Verb.POST;
        if (sMethod.equals("put")) return Verb.PUT;
        if (sMethod.equals("delete")) return Verb.DELETE;
        if (sMethod.equals("options")) return Verb.OPTIONS;
        if (sMethod.equals("head")) return Verb.HEAD;
        return Verb.GET;
    }

    void setResponse(HttpResponse resp) throws ClientException, IOException {
        RequestContext context = RequestContext.getCurrentContext();

        context.setResponseStatusCode(resp.getStatus());
        if (resp.hasEntity()) {
            context.setResponseDataStream(resp.getInputStream());
        }

        String contentEncoding = null;
        Collection<String> contentEncodingHeader = resp.getHeaders().get(CONTENT_ENCODING);
        if (contentEncodingHeader != null && !contentEncodingHeader.isEmpty()) {
            contentEncoding = contentEncodingHeader.iterator().next();
        }

        if (contentEncoding != null && HTTPRequestUtils.getInstance().isGzipped(contentEncoding)) {
            context.setResponseGZipped(true);
        } else {
            context.setResponseGZipped(false);
        }

        if (Debug.debugRequest()) {
            for (String key : resp.getHeaders().keySet()) {
                boolean isValidHeader = isValidHeader(key);

                Collection<String> list = resp.getHeaders().get(key);
                for (String header : list) {
                    context.addOriginResponseHeader(key, header);

                    if (key.equalsIgnoreCase("content-length"))
                        context.setOriginContentLength(header);

                    if (isValidHeader) {
                        context.addZuulResponseHeader(key, header);
                        Debug.addRequestDebug(String.format("ORIGIN_RESPONSE:: < %s  %s", key, header));
                    }
                }
            }

            if (context.getResponseDataStream() != null) {
                byte[] origBytes = IOUtils.toByteArray(context.getResponseDataStream());
                InputStream inStream = new ByteArrayInputStream(origBytes);
                if (context.getResponseGZipped())
                    inStream = new GZIPInputStream(inStream);
                String responseEntity = IOUtils.toString(inStream);
                Debug.addRequestDebug("ORIGIN_RESPONSE:: < "+responseEntity);
                context.setResponseDataStream(new ByteArrayInputStream(origBytes));
            }

        } else {
            for (String key : resp.getHeaders().keySet()) {
                boolean isValidHeader = isValidHeader(key);
                Collection<java.lang.String> list = resp.getHeaders().get(key);
                for (String header : list) {
                    context.addOriginResponseHeader(key, header);

                    if (key.equalsIgnoreCase("content-length"))
                        context.setOriginContentLength(header);

                    if (isValidHeader) {
                        context.addZuulResponseHeader(key, header);
                    }
                }
            }
        }


    }

    boolean isValidHeader(String headerName) {
        switch (headerName.toLowerCase()) {
            case "connection":
            case "content-length":
            case "content-encoding":
            case "server":
            case "transfer-encoding":
                return false;
            default:
                return true;
        }
    }

}


