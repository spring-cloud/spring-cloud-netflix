/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.cloud.netflix.feign.encoding.proto;

import feign.RequestTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.FilteredClassPathRunner;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * Test {@link SpringEncoder} when protobuf is not in classpath
 *
 * @author ScienJus
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions("protobuf-*.jar")
public class ProtobufNotInClasspathTest {

    @Test
    public void testEncodeWhenProtobufNotInClasspath() {
        ObjectFactory<HttpMessageConverters> converters = new ObjectFactory<HttpMessageConverters>() {
            @Override
            public HttpMessageConverters getObject() throws BeansException {
                return new HttpMessageConverters(new StringHttpMessageConverter());
            }
        };
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.method("POST");
        new SpringEncoder(converters).encode("a=b", String.class, requestTemplate);
    }

}
