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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.support;

import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.Servlet30WrapperFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SendForwardFilter;

import com.netflix.zuul.ZuulFilter;

/**
 * @author Spencer Gibb
 */
public class FilterConstants {

	// KEY constants -----------------------------------

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.pre.ServletDetectionFilter}
	 */
	public static final String IS_DISPATCHER_SERVLET_REQUEST_KEY = "isDispatcherServletRequest";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.SendForwardFilter}
	 */
	public static final String FORWARD_TO_KEY = "forward.to";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in TODO: determine use
	 */
	public static final String PROXY_KEY = "proxy";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter}
	 */
	public static final String REQUEST_ENTITY_KEY = "requestEntity";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in to override the path of the request.
	 */
	public static final String REQUEST_URI_KEY = "requestURI";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter}
	 */
	public static final String RETRYABLE_KEY = "retryable";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter}
	 */
	public static final String ROUTING_DEBUG_KEY = "routingDebug";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter}
	 */
	public static final String SERVICE_ID_KEY = "serviceId";

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter}
	 */
	public static final String LOAD_BALANCER_KEY = "loadBalancerKey";

	// ORDER constants -----------------------------------

	/**
	 * Filter Order for {@link DebugFilter#filterOrder()}
	 */
	public static final int DEBUG_FILTER_ORDER = 1;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.pre.FormBodyWrapperFilter#filterOrder()}
	 */
	public static final int FORM_BODY_WRAPPER_FILTER_ORDER = -1;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter}
	 */
	public static final int PRE_DECORATION_FILTER_ORDER = 5;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter#filterOrder()}
	 */
	public static final int RIBBON_ROUTING_FILTER_ORDER = 10;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter#filterOrder()}
	 */
	public static final int SEND_ERROR_FILTER_ORDER = 0;

	/**
	 * Filter Order for {@link SendForwardFilter#filterOrder()}
	 */
	public static final int SEND_FORWARD_FILTER_ORDER = 500;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter#filterOrder()}
	 */
	public static final int SEND_RESPONSE_FILTER_ORDER = 1000;

	/**
	 * Filter Order for {@link org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter#filterOrder()}
	 */
	public static final int SIMPLE_HOST_ROUTING_FILTER_ORDER = 100;

	/**
	 * filter order for {@link Servlet30WrapperFilter#filterOrder()}
	 */
	public static final int SERVLET_30_WRAPPER_FILTER_ORDER = -2;

	/**
	 * filter order for {@link org.springframework.cloud.netflix.zuul.filters.pre.ServletDetectionFilter#filterOrder()}
	 */
	public static final int SERVLET_DETECTION_FILTER_ORDER = -3;

	// Zuul Filter TYPE constants -----------------------------------

	/**
	 * {@link ZuulFilter#filterType()} error type.
	 */
	public static final String ERROR_TYPE = "error";

	/**
	 * {@link ZuulFilter#filterType()} post type.
	 */
	public static final String POST_TYPE = "post";

	/**
	 * {@link ZuulFilter#filterType()} pre type.
	 */
	public static final String PRE_TYPE = "pre";

	/**
	 * {@link ZuulFilter#filterType()} route type.
	 */
	public static final String ROUTE_TYPE = "route";

	// OTHER constants -----------------------------------

	/**
	 * Zuul {@link com.netflix.zuul.context.RequestContext} key for use in {@link org.springframework.cloud.netflix.zuul.filters.route.SendForwardFilter}
	 */
	public static final String FORWARD_LOCATION_PREFIX = "forward:";

	/**
	 * default http port
	 */
	public static final int HTTP_PORT = 80;

	/**
	 * default https port
	 */
	public static final int HTTPS_PORT = 443;

	/**
	 * http url scheme
	 */
	public static final String HTTP_SCHEME = "http";

	/**
	 * https url scheme
	 */
	public static final String HTTPS_SCHEME = "https";

	// HEADER constants -----------------------------------

	/**
	 * X-* Header for the matching url. Used when routes use a url rather than serviceId
	 */
	public static final String SERVICE_HEADER = "X-Zuul-Service";

	/**
	 * X-* Header for the matching serviceId
	 */
	public static final String SERVICE_ID_HEADER = "X-Zuul-ServiceId";

	/**
	 * X-Forwarded-For Header
	 */
	public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

	/**
	 * X-Forwarded-Host Header
	 */
	public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

	/**
	 * X-Forwarded-Prefix Header
	 */
	public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

	/**
	 * X-Forwarded-Port Header
	 */
	public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

	/**
	 * X-Forwarded-Proto Header
	 */
	public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

	/**
	 * X-Zuul-Debug Header
	 */
	public static final String X_ZUUL_DEBUG_HEADER = "X-Zuul-Debug-Header";

	private FilterConstants() {
		throw new AssertionError("Must not instantiate constant utility class");
	}

}
