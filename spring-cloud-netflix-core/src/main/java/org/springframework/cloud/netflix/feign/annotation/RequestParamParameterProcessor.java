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

import feign.MethodMetadata;
import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link RequestParam} parameter processor.
 *
 * @author Jakub Narloch
 * @see AnnotatedParameterProcessor
 */
public class RequestParamParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestParam> ANNOTATION = RequestParam.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation annotation) {
        String name = ANNOTATION.cast(annotation).value();
        checkState(emptyToNull(name) != null,
                "RequestParam.value() was empty on parameter %s", context.getParameterIndex());
        context.setParameterName(name);

        MethodMetadata data = context.getMethodMetadata();
        Collection<String> query = context.setTemplateParameter(name, data.template().queries().get(name));
        data.template().query(name, query);
        return true;
    }
}
