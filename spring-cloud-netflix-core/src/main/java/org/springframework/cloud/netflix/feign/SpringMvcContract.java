package org.springframework.cloud.netflix.feign;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.web.bind.annotation.RequestMapping;

import feign.Contract;
import feign.MethodMetadata;

/**
 * Created by sgibb on 6/27/14.
 */
public class SpringMvcContract extends Contract.BaseContract {
    static final String ACCEPT = "Accept";
    static final String CONTENT_TYPE = "Content-Type";

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
        RequestMapping mapping = RequestMapping.class.cast(methodAnnotation);
        if (mapping != null) {
            //HTTP Method
            checkOne(method, mapping.method(), "method");
            data.template().method(mapping.method()[0].name());

            //path
            checkOne(method, mapping.value(), "value");

            String methodAnnotationValue = mapping.value()[0];
            String pathValue = emptyToNull(methodAnnotationValue);
            checkState(pathValue != null, "value was empty on method %s", method.getName());
            if (!methodAnnotationValue.startsWith("/") && !data.template().toString().endsWith("/")) {
                methodAnnotationValue = "/" + methodAnnotationValue;
            }
            data.template().append(methodAnnotationValue);

            //produces
            checkAtMostOne(method, mapping.produces(), "produces");
            String[] serverProduces = mapping.produces();
            String clientAccepts = serverProduces.length == 0 ? null: emptyToNull(serverProduces[0]);
            if (clientAccepts != null) {
                data.template().header(ACCEPT, clientAccepts);
            }

            //consumes
            checkAtMostOne(method, mapping.consumes(), "consumes");
            String[] serverConsumes = mapping.consumes();
            String clientProduces = serverConsumes.length == 0 ? null: emptyToNull(serverConsumes[0]);
            if (clientProduces != null) {
                data.template().header(CONTENT_TYPE, clientProduces);
            }

            //headers
            //TODO: only supports one header value per key
            if (mapping.headers() != null && mapping.headers().length > 0)
            for (String header : mapping.headers()) {
                int colon = header.indexOf(':');
                data.template().header(header.substring(0, colon), header.substring(colon + 2));
            }
        }
    }

    private void checkAtMostOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && (values.length == 0 || values.length == 1),
                "Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
                values == null ? null : Arrays.asList(values));
    }

    private void checkOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && values.length == 1,
                "Method %s can only contain 1 %s field. Found: %s", method.getName(), fieldName,
                values == null ? null : Arrays.asList(values));
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
        //TODO: support spring parameter annotations?
        return false;
    }
}
