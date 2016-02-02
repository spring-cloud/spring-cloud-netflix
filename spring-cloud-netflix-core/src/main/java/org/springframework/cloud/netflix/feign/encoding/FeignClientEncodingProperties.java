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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Feign encoding properties.
 *
 * @author Jakub Narloch
 */
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

    public FeignClientEncodingProperties() {
    }

    public String[] getMimeTypes() {
        return this.mimeTypes;
    }

    public int getMinRequestSize() {
        return this.minRequestSize;
    }

    public void setMimeTypes(String[] mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public void setMinRequestSize(int minRequestSize) {
        this.minRequestSize = minRequestSize;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FeignClientEncodingProperties))
            return false;
        final FeignClientEncodingProperties other = (FeignClientEncodingProperties) o;
        if (!other.canEqual((Object) this))
            return false;
        if (!java.util.Arrays.deepEquals(this.mimeTypes, other.mimeTypes))
            return false;
        if (this.minRequestSize != other.minRequestSize)
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.util.Arrays.deepHashCode(this.mimeTypes);
        result = result * PRIME + this.minRequestSize;
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof FeignClientEncodingProperties;
    }

    public String toString() {
        return "org.springframework.cloud.netflix.feign.encoding.FeignClientEncodingProperties(mimeTypes="
                + java.util.Arrays.deepToString(this.mimeTypes) + ", minRequestSize="
                + this.minRequestSize + ")";
    }
}
