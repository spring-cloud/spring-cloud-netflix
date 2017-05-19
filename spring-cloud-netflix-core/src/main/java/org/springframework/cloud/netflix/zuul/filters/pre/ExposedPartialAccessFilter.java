/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.netflix.zuul.filters.ExposedPartialProperty;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * Pre {@link ZuulFilter} that determines the access to a route downstream based on partial-exposed property.
 *
 * @author Kevin Van Houtte
 */
public class ExposedPartialAccessFilter extends ZuulFilter {

    private final ZuulProperties properties;

    private final ExposedPartialProperty partialProperties;

    public ExposedPartialAccessFilter(ZuulProperties properties, ExposedPartialProperty partialProperties) {
        this.properties = properties;
        this.partialProperties = partialProperties;

    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        if (StringUtils.isEmpty(request.getServletPath()) || !request.getServletPath().contains("/")) {
            return false;
        }
        String key = getKey(request.getServletPath().split("/"));
        ZuulProperties.ZuulRoute accessLevel = properties.getRoutes().get(key);
        return accessLevel != null && "partial-exposed".equals(accessLevel.getSecureAccessLevel());
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String header = request.getHeader("Authorization");
        String path = request.getServletPath();
        String key = getKey(request.getServletPath().split("/"));
        if (StringUtils.isEmpty(header) || !header.startsWith("Bearer")) {
            List<String> partialPaths = partialProperties.getPaths().get(key);
            if (partialPaths == null || partialPaths.isEmpty()) {
                setFailedRequest("Forbidden", 403);
            } else {
                Boolean pathFound = partialPaths.contains(path);
                if (pathFound) {
                    return null;
                } else {
                    setFailedRequest("Forbidden", 403);
                }
            }
        }
        return null;
    }

    private String getKey(String[] parts) {
        return parts[1];
    }

    private void setFailedRequest(String body, int code) {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setResponseStatusCode(code);
        if (ctx.getResponseBody() == null) {
            ctx.setResponseBody(body);
            ctx.setSendZuulResponse(false);
            throw new AccessDeniedException("Code: " + code + ", " + body); //optional
        }
    }
}
