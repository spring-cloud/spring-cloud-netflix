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

package org.springframework.cloud.netflix.zuul.util;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.IS_DISPATCHER_SERVLET_REQUEST_KEY;

import java.io.Closeable;
import java.util.List;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.context.RequestContext;

public class RequestUtils {

	/**
	 * @deprecated use {@link org.springframework.cloud.netflix.zuul.filters.support.FilterConstants#IS_DISPATCHER_SERVLET_REQUEST_KEY}
	 */
	@Deprecated
	public static final String IS_DISPATCHERSERVLETREQUEST = IS_DISPATCHER_SERVLET_REQUEST_KEY;
	
	public static boolean isDispatcherServletRequest() {
		return RequestContext.getCurrentContext().getBoolean(IS_DISPATCHER_SERVLET_REQUEST_KEY);
	}
	
	public static boolean isZuulServletRequest() {
		//extra check for dispatcher since ZuulServlet can run from ZuulController
		return !isDispatcherServletRequest() && RequestContext.getCurrentContext().getZuulEngineRan();
	}

	
	/**
	 * Register a {@link Runnable} to be invoked after the request is fully completed.
	 * Can be used to dispose resources allocated during processing.
	 * <p>
	 * Note that the RequestContext has already been cleared when the callback method is invoked.
	 * The callback must therefore record all the information it needs for its processing at the 
	 * time it is registered.
	 * 
	 * @param callback the @link Runnable} to invoke after request processing is fully completed.
	 */
	public static void registerAfterRequestCompletionCallback(Runnable callback) {
		@SuppressWarnings("unchecked")
		List<Runnable> callbacks = (List<Runnable>) RequestContext.getCurrentContext().get(FilterConstants.COMPLETION_CALLBACKS_KEY);
		if (callbacks==null) {
			throw new IllegalStateException(FilterConstants.COMPLETION_CALLBACKS_KEY + " attribute not found. It should have been set by ZuulController before request execution. Did a filter clear the RequestContext during processing?");
		}
		callbacks.add(callback);
	}
	
	/**
	 * Convenience method used to register a callback to close the given {@code closeable} at 
	 * request completion.
	 * 
	 * @param closeable the closeable to close at request completion.
	 */
	public static void closeAfterRequestCompletion(final Closeable closeable) {
		if (closeable!=null) {
    		RequestUtils.registerAfterRequestCompletionCallback(new Runnable() {
    			@Override
    			public void run() {
    				try {
    					closeable.close();
    				}
    				catch(Exception ex) {
    					ReflectionUtils.rethrowRuntimeException(ex);
    				}
    			}	
    		});
		}
	}
}
