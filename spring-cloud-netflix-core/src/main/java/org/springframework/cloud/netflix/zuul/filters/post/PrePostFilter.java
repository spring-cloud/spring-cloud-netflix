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

package org.springframework.cloud.netflix.zuul.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * Indicate that "post" filter phase started.
 *
 * @author Denys Kurylenko
 */
public class PrePostFilter extends ZuulFilter {
  @Override
  public String filterType() {
    return "post";
  }

  @Override
  public int filterOrder() {
    return -1000;
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  public Object run() {
    final RequestContext ctx = RequestContext.getCurrentContext();
    ctx.set("post.filters.started", Boolean.TRUE);
    return null;
  }
}
