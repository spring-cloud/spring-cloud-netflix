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

package org.springframework.cloud.netflix.ribbon.apache;

import static org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer.Runner.customize;

import java.net.URI;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;

/**
 * @author Christian Lohmann
 */
public class RibbonApacheHttpRequest extends ContextAwareRequest implements Cloneable {

	public RibbonApacheHttpRequest(RibbonCommandContext context) {
		super(context);
	}

	public HttpUriRequest toRequest(final RequestConfig requestConfig) {
		final RequestBuilder builder = RequestBuilder.create(this.context.getMethod());
		builder.setUri(this.uri);
		for (final String name : this.context.getHeaders().keySet()) {
			final List<String> values = this.context.getHeaders().get(name);
			for (final String value : values) {
				builder.addHeader(name, value);
			}
		}

		for (final String name : this.context.getParams().keySet()) {
			final List<String> values = this.context.getParams().get(name);
			for (final String value : values) {
				builder.addParameter(name, value);
			}
		}

		if (this.context.getRequestEntity() != null) {
			final BasicHttpEntity entity;
			entity = new BasicHttpEntity();
			entity.setContent(this.context.getRequestEntity());
			// if the entity contentLength isn't set, transfer-encoding will be set
			// to chunked in org.apache.http.protocol.RequestContent. See gh-1042
			Long contentLength = this.context.getContentLength();
			if ("GET".equals(this.context.getMethod()) && (contentLength == null || contentLength < 0)) {
				entity.setContentLength(0);
			} else if (contentLength != null) {
				entity.setContentLength(contentLength);
			}
			builder.setEntity(entity);
		}

		customize(this.context.getRequestCustomizers(), builder);

		builder.setConfig(requestConfig);
		return builder.build();
	}

	public RibbonApacheHttpRequest withNewUri(URI uri) {
		return new RibbonApacheHttpRequest(newContext(uri));
	}

}
