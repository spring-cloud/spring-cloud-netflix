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

package org.springframework.cloud.netflix.feign.encoding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Feign encoding properties.
 *
 * @author Jakub Narloch
 */
@Data
@ConfigurationProperties("feign.compression.request")
public class FeignClientEncodingProperties {

    /**
     * The list of supported mime types.
     */
    private String[] mimeTypes = new String[]{"text/xml", "application/xml", "application/json"};

    /**
     * The minimum threshold content size.
     */
    private int minRequestSize = 2048;
}
