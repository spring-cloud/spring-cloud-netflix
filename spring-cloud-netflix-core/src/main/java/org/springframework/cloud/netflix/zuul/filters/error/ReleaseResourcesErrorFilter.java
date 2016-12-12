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

package org.springframework.cloud.netflix.zuul.filters.error;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * Releases underneath connections created by "route" filters, but not released by "post" filters.
 *
 * In most exceptional cases "error" filters are getting executed before "post" filters, therefore closing input stream blindly might lead to
 * unpredictable behaviour of "post" filters.
 *
 * This filter coordinates work with {@link org.springframework.cloud.netflix.zuul.filters.post.PrePostFilter} to give "post" filters chance to be executed first
 *
 * TODO: Better fix would to close input stream in {@link com.netflix.zuul.http.ZuulServlet} or {@link org.springframework.cloud.netflix.zuul.web.ZuulController}
 *
 * @author Denys Kurylenko
 */
public class ReleaseResourcesErrorFilter extends ZuulFilter {

  @Override
  public String filterType() {
    return "error";
  }

  @Override
  public int filterOrder() {
    return 0;
  }

  @Override
  public boolean shouldFilter() {
    // don't close connection unless "post" filters attempted to read the data
    final RequestContext ctx = RequestContext.getCurrentContext();
    return ctx.getResponseDataStream() != null && (ctx.getResponse().isCommitted() || ctx.getBoolean("post.filters.started"));
  }

  @Override
  public Object run() {
    final InputStream is = RequestContext.getCurrentContext().getResponseDataStream();
    try {
      if (is != null) {
        is.close();
      }
    }
    catch (IOException ex) {
    }
    return null;
  }
}
