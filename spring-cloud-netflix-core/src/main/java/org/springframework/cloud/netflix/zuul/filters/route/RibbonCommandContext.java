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

package org.springframework.cloud.netflix.zuul.filters.route;

import lombok.Value;
import org.springframework.util.MultiValueMap;

import java.io.InputStream;

/**
 * @author Spencer Gibb
 */
@Value
public class RibbonCommandContext {
	private final String serviceId;
	private final String verb;
	private final String uri;
	private final Boolean retryable;
	private final MultiValueMap<String, String> headers;
	private final MultiValueMap<String, String> params;
	private final InputStream requestEntity;
}
