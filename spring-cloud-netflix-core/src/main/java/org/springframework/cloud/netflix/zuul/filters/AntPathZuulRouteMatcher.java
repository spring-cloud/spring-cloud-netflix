/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A base implementation of {@link ZuulRouteMatcher} that uses {@link AntPathMatcher} for matching the request URI
 * with configured routes.
 *
 * @author Jakub Narloch
 */
@CommonsLog
public class AntPathZuulRouteMatcher implements ZuulRouteMatcher {

    private final PathMatcher pathMatcher = new AntPathMatcher();

    private final AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

    @Override
    public void setRoutes(Map<String, ZuulRoute> routes) {
        this.routes.set(routes);
    }

    @Override
    public Map<String, ZuulRoute> getRoutes() {
        return routes.get();
    }

    @Override
    public ZuulRoute getMatchingRoute(String path) {
        for(Map.Entry<String, ZuulRoute> entry : this.routes.get().entrySet()) {
            if(pathMatcher.match(entry.getKey(), path)) {
                if(log.isDebugEnabled()) {
                    log.debug("Matching pattern:" + entry.getKey());
                }
                return entry.getValue();
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("No route for path: " + path + " has been found");
        }
        return null;
    }
}
