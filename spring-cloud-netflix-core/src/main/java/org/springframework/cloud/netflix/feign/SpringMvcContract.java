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

package org.springframework.cloud.netflix.feign;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import feign.Contract;
import feign.MethodMetadata;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * @author Spencer Gibb
 */
public class SpringMvcContract extends Contract.BaseContract {
	static final String ACCEPT = "Accept";
	static final String CONTENT_TYPE = "Content-Type";

	@Override
	protected void processAnnotationOnMethod(MethodMetadata data,
			Annotation methodAnnotation, Method method) {
		RequestMapping mapping = RequestMapping.class.cast(methodAnnotation);
		if (mapping != null) {
			// HTTP Method
			checkOne(method, mapping.method(), "method");
			data.template().method(mapping.method()[0].name());

			// path
			checkOne(method, mapping.value(), "value");

			String methodAnnotationValue = mapping.value()[0];
			String pathValue = emptyToNull(methodAnnotationValue);
			checkState(pathValue != null, "value was empty on method %s",
					method.getName());
			if (!methodAnnotationValue.startsWith("/")
					&& !data.template().toString().endsWith("/")) {
				methodAnnotationValue = "/" + methodAnnotationValue;
			}
			data.template().append(methodAnnotationValue);

			// produces
			checkAtMostOne(method, mapping.produces(), "produces");
			String[] serverProduces = mapping.produces();
			String clientAccepts = serverProduces.length == 0 ? null
					: emptyToNull(serverProduces[0]);
			if (clientAccepts != null) {
				data.template().header(ACCEPT, clientAccepts);
			}

			// consumes
			checkAtMostOne(method, mapping.consumes(), "consumes");
			String[] serverConsumes = mapping.consumes();
			String clientProduces = serverConsumes.length == 0 ? null
					: emptyToNull(serverConsumes[0]);
			if (clientProduces != null) {
				data.template().header(CONTENT_TYPE, clientProduces);
			}

			// headers
			// TODO: only supports one header value per key
			if (mapping.headers() != null && mapping.headers().length > 0) {
				for (String header : mapping.headers()) {
					int colon = header.indexOf(':');
					data.template().header(header.substring(0, colon),
							header.substring(colon + 2));
				}
			}
		}
	}

	private void checkAtMostOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && (values.length == 0 || values.length == 1),
				"Method %s can only contain at most 1 %s field. Found: %s",
				method.getName(), fieldName,
				values == null ? null : Arrays.asList(values));
	}

	private void checkOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && values.length == 1,
				"Method %s can only contain 1 %s field. Found: %s", method.getName(),
				fieldName, values == null ? null : Arrays.asList(values));
	}

	@Override
	protected boolean processAnnotationsOnParameter(MethodMetadata data,
			Annotation[] annotations, int paramIndex) {
		boolean isHttpAnnotation = false;
		// TODO: support spring parameter annotations?
		for (Annotation parameterAnnotation : annotations) {
			Class<? extends Annotation> annotationType = parameterAnnotation
					.annotationType();
			if (annotationType == PathVariable.class) {
				String name = PathVariable.class.cast(parameterAnnotation).value();
				checkState(emptyToNull(name) != null,
						"PathVariable annotation was empty on param %s.", paramIndex);
				nameParam(data, name, paramIndex);
				isHttpAnnotation = true;
				String varName = '{' + name + '}';
				if (data.template().url().indexOf(varName) == -1
						&& !searchMapValues(data.template().queries(), varName)
						&& !searchMapValues(data.template().headers(), varName)) {
					data.formParams().add(name);
				}
			}
			else if (annotationType == RequestParam.class) {
				String name = RequestParam.class.cast(parameterAnnotation).value();
				checkState(emptyToNull(name) != null,
						"QueryParam.value() was empty on parameter %s", paramIndex);
				Collection<String> query = addTemplatedParam(data.template().queries()
						.get(name), name);
				data.template().query(name, query);
				nameParam(data, name, paramIndex);
				isHttpAnnotation = true;
			}
			else if (annotationType == RequestHeader.class) {
				String name = RequestHeader.class.cast(parameterAnnotation).value();
				checkState(emptyToNull(name) != null,
						"HeaderParam.value() was empty on parameter %s", paramIndex);
				Collection<String> header = addTemplatedParam(data.template().headers()
						.get(name), name);
				data.template().header(name, header);
				nameParam(data, name, paramIndex);
				isHttpAnnotation = true;
			}/*
			 * else if (annotationType == FormParam.class) { String name =
			 * FormParam.class.cast(parameterAnnotation).value();
			 * checkState(emptyToNull(name) != null,
			 * "FormParam.value() was empty on parameter %s", paramIndex);
			 * data.formParams().add(name); nameParam(data, name, paramIndex);
			 * isHttpAnnotation = true; }
			 */

		}
		return isHttpAnnotation;
	}

	private <K, V> boolean searchMapValues(Map<K, Collection<V>> map, V search) {
		Collection<Collection<V>> values = map.values();
		if (values == null) {
			return false;
		}

		for (Collection<V> entry : values) {
			if (entry.contains(search)) {
				return true;
			}
		}

		return false;
	}
}
