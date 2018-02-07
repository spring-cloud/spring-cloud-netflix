/*
 * Copyright 2013-2018 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;

/**
 * Provides basic utilities for {@link org.apache.http.client.HttpClient}
 * @author Ryan Baxter
 */
public class HttpClientUtils {

	/**
	 * Creates an new {@link HttpEntity} by copying the {@link HttpEntity} from the {@link HttpResponse}.
	 * This method will close the response after copying the entity.
	 * @param response The response to create the {@link HttpEntity} from
	 * @return A new {@link HttpEntity}
	 * @throws IOException thrown if there is a problem closing the response.
	 */
	public static HttpEntity createEntity(HttpResponse response) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(
				EntityUtils.toByteArray(response.getEntity()));
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(is);
		entity.setContentLength(response.getEntity().getContentLength());
		if(CloseableHttpResponse.class.isInstance(response)) {
			((CloseableHttpResponse)response).close();
		}
		return entity;
	}
}
