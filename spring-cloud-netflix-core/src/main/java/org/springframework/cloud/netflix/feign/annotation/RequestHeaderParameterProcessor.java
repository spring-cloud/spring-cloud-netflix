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

package org.springframework.cloud.netflix.feign.annotation;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.RequestHeader;

import feign.MethodMetadata;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link RequestHeader} parameter processor.
 *
 * @author Jakub Narloch
 * @see AnnotatedParameterProcessor
 */
public class RequestHeaderParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestHeader> ANNOTATION = RequestHeader.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation annotation) {
        String name = ANNOTATION.cast(annotation).value();
        checkState(emptyToNull(name) != null,
                "RequestHeader.value() was empty on parameter %s", context.getParameterIndex());
        context.setParameterName(name);

        MethodMetadata data = context.getMethodMetadata();
        Collection<String> header = context.setTemplateParameter(name, data.template().headers().get(name));
        data.template().header(name, header);
        return true;
    }
}
