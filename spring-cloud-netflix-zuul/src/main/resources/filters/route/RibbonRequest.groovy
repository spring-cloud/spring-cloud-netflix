package filters.route

import com.netflix.client.ClientException
import com.netflix.client.ClientFactory
import com.netflix.client.IClient
import com.netflix.client.http.HttpRequest
import com.netflix.client.http.HttpResponse
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.niws.client.http.RestClient
import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.Debug
import com.netflix.zuul.context.RequestContext
import com.netflix.zuul.exception.ZuulException
import com.netflix.zuul.util.HTTPRequestUtils
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.netflix.zuul.RibbonCommand

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MultivaluedMap
import java.util.zip.GZIPInputStream

import static HttpRequest.Verb
import static org.springframework.cloud.netflix.feign.FeignConfigurer.setServiceListClassAndVIP

class RibbonRequest extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RibbonRequest.class);

    public static final String CONTENT_ENCODING = "Content-Encoding";

    @Override
    String filterType() {
        return 'route'
    }

    @Override
    int filterOrder() {
        return 10
    }

    boolean shouldFilter() {
        def ctx = RequestContext.currentContext
        return (ctx.getRouteHost() == null && ctx.get("serviceId") != null && ctx.sendZuulResponse())
    }

    Object run() {
        RequestContext context = RequestContext.currentContext
        HttpServletRequest request = context.getRequest();

        MultivaluedMap<String, String> headers = buildZuulRequestHeaders(request)
        MultivaluedMap<String, String> params = buildZuulRequestQueryParams(request)
        Verb verb = getVerb(request);
        Object requestEntity = getRequestBody(request)

        def serviceId = context.get("serviceId")

        //TODO: can this be set be default? or an implementation of an interface?
        setServiceListClassAndVIP(serviceId)

        IClient restClient = ClientFactory.getNamedClient(serviceId);

        String uri = request.getRequestURI()
        if (context.requestURI != null) {
            uri = context.requestURI
        }
        //remove double slashes
        uri = uri.replace("//", "/")

        HttpResponse response = forward(restClient, verb, uri, headers, params, requestEntity)
        setResponse(response)
        return response
    }



    void debug(RestClient restClient, Verb verb, uri, MultivaluedMap<String, String> headers, MultivaluedMap<String, String> params, InputStream requestEntity) {

        if (Debug.debugRequest()) {

            headers.each {
                Debug.addRequestDebug("ZUUL:: > ${it.key}  ${it.value[0]}")
            }
            String query = ""
            params.each {
                it.value.each { v ->
                    query += it.key + "=" + v + "&"
                }
            }

            Debug.addRequestDebug("ZUUL:: > ${verb.verb()}  ${uri}?${query} HTTP/1.1")
            RequestContext ctx = RequestContext.getCurrentContext()
            if (!ctx.isChunkedRequestBody()) {
                if (requestEntity != null) {
                    debugRequestEntity(ctx.request.getInputStream())
                }
            }
        }
    }

    void debugRequestEntity(InputStream inputStream) {
        if (!Debug.debugRequestHeadersOnly()) {
            String entity = inputStream.getText()
            Debug.addRequestDebug("ZUUL:: > ${entity}")
        }
    }



    def HttpResponse forward(RestClient restClient, Verb verb, uri, MultivaluedMap<String, String> headers,
                             MultivaluedMap<String, String> params, InputStream requestEntity) {
        debug(restClient, verb, uri, headers, params, requestEntity)

        RibbonCommand command = new RibbonCommand(restClient, verb, uri, headers, params, requestEntity);
        try {
            HttpResponse response = command.execute();
            return response
        } catch (HystrixRuntimeException e) {
            if (e?.fallbackException?.cause instanceof ClientException) {
                ClientException ex = e.fallbackException.cause as ClientException
                throw new ZuulException(ex, "Forwarding error", 500, ex.getErrorType().toString())
            }
            throw new ZuulException(e, "Forwarding error", 500, e.failureType.toString())
        }

    }


    def getRequestBody(HttpServletRequest request) {
        Object requestEntity = null;
        try {
            requestEntity = RequestContext.currentContext.requestEntity
            if (requestEntity == null) {
                requestEntity = request.getInputStream();
            }
        } catch (IOException e) {
            LOG.error(e);
        }

        return requestEntity
    }



    def MultivaluedMap<String, String> buildZuulRequestQueryParams(HttpServletRequest request) {

        Map<String, List<String>> map = HTTPRequestUtils.getInstance().getQueryParams()

        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        if (map == null) return params;
        map.entrySet().each {
            it.value.each { v ->
                params.add(it.key, v)
            }

        }
        return params
    }


    def MultivaluedMap<String, String> buildZuulRequestHeaders(HttpServletRequest request) {

        RequestContext context = RequestContext.currentContext

        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<String, String>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames?.hasMoreElements()) {
            String name = (String) headerNames.nextElement();
            String value = request.getHeader(name);
            if (!name.toLowerCase().contains("content-length")) headers.putSingle(name, value);
        }
        Map zuulRequestHeaders = context.getZuulRequestHeaders();

        zuulRequestHeaders.keySet().each {
            headers.putSingle((String) it, (String) zuulRequestHeaders[it])
        }

        headers.putSingle("accept-encoding", "deflate, gzip")

        if (headers.containsKey("transfer-encoding"))
            headers.remove("transfer-encoding")

        return headers
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

    void setResponse(HttpResponse resp) {
        RequestContext context = RequestContext.getCurrentContext()

        context.setResponseStatusCode(resp.getStatus());
        if (resp.hasEntity()) {
            context.responseDataStream = resp.inputStream;
        }

        String contentEncoding = resp.getHeaders().get(CONTENT_ENCODING)?.first();

        if (contentEncoding != null && HTTPRequestUtils.getInstance().isGzipped(contentEncoding)) {
            context.setResponseGZipped(true);
        } else {
            context.setResponseGZipped(false);
        }

        if (Debug.debugRequest()) {
            resp.getHeaders().keySet().each { key ->
                boolean isValidHeader = isValidHeader(key)

                Collection<String> list = resp.getHeaders().get(key)
                list.each { header ->
                    context.addOriginResponseHeader(key, header)

                    if (key.equalsIgnoreCase("content-length"))
                        context.setOriginContentLength(header);

                    if (isValidHeader) {
                        context.addZuulResponseHeader(key, header);
                        Debug.addRequestDebug("ORIGIN_RESPONSE:: < ${key}  ${header}")
                    }
                }
            }

            if (context.responseDataStream) {
                byte[] origBytes = context.getResponseDataStream().bytes
                InputStream inStream = new ByteArrayInputStream(origBytes);
                if (context.getResponseGZipped())
                    inStream = new GZIPInputStream(inStream);
                String responseEntity = inStream.getText()
                Debug.addRequestDebug("ORIGIN_RESPONSE:: < ${responseEntity}")
                context.setResponseDataStream(new ByteArrayInputStream(origBytes))
            }

        } else {
            resp.getHeaders().keySet().each { key ->
                boolean isValidHeader = isValidHeader(key)
                Collection<java.lang.String> list = resp.getHeaders().get(key)
                list.each { header ->
                    context.addOriginResponseHeader(key, header)

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
                return false
            default:
                return true
        }
    }

}


