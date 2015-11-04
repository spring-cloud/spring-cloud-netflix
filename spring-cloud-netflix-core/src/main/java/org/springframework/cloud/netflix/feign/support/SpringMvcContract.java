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

package org.springframework.cloud.netflix.feign.support;

import feign.Contract;
import feign.MethodMetadata;
import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * @author Spencer Gibb
 */
public class SpringMvcContract extends Contract.BaseContract {

	private static final String ACCEPT = "Accept";

	private static final String CONTENT_TYPE = "Content-Type";

	private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;

	public SpringMvcContract() {
		this(Collections.<AnnotatedParameterProcessor>emptyList());
	}

	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
		Assert.notNull(annotatedParameterProcessors, "Parameter processors can not be null.");

		List<AnnotatedParameterProcessor> processors;
		if(!annotatedParameterProcessors.isEmpty()) {
			processors = new ArrayList<>(annotatedParameterProcessors);
		} else {
			processors = getDefaultAnnotatedArgumentsProcessors();
		}
		this.annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
	}

	@Override
	public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
		MethodMetadata md = super.parseAndValidateMetadata(targetType, method);

		RequestMapping classAnnotation = targetType.getAnnotation(RequestMapping.class);
		if (classAnnotation != null) {
			// Prepend path from class annotation if specified
			if (classAnnotation.value().length > 0) {
				String pathValue = emptyToNull(classAnnotation.value()[0]);
				checkState(pathValue != null, "RequestMapping.value() was empty on type %s",
						method.getDeclaringClass().getName());
				if (!pathValue.startsWith("/")) {
					pathValue = "/" + pathValue;
				}
				md.template().insert(0, pathValue);
			}

			// produces - use from class annotation only if method has not specified this
			if(!md.template().headers().containsKey(ACCEPT)) {
				parseProduces(md, method, classAnnotation);
			}

			// consumes -- use from class annotation only if method has not specified this
			if(!md.template().headers().containsKey(CONTENT_TYPE)) {
				parseConsumes(md, method, classAnnotation);
			}

			// headers -- class annotation is inherited to methods, always write these if present
			parseHeaders(md, method, classAnnotation);
		}
		return md;
	}

	@Override
	protected void processAnnotationOnMethod(MethodMetadata data,
			Annotation methodAnnotation, Method method) {
		if (!(methodAnnotation instanceof RequestMapping)) {
			return;
		}

		RequestMapping methodMapping = RequestMapping.class.cast(methodAnnotation);
		// HTTP Method
		checkOne(method, methodMapping.method(), "method");
		data.template().method(methodMapping.method()[0].name());

		// path
		checkAtMostOne(method, methodMapping.value(), "value");
		if(methodMapping.value().length > 0) {
			String pathValue = emptyToNull(methodMapping.value()[0]);
			if (pathValue != null) {
				// Append path from @RequestMapping if value is present on method
				if (!pathValue.startsWith("/") && !data.template().toString().endsWith("/")) {
					pathValue = "/" + pathValue;
				}
				data.template().append(pathValue);
			}
		}

		// produces
		parseProduces(data, method, methodMapping);

		// consumes
		parseConsumes(data, method, methodMapping);

		// headers
		parseHeaders(data, method, methodMapping);
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
	protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
		boolean isHttpAnnotation = false;

		AnnotatedParameterProcessor.AnnotatedParameterContext context =
				new SimpleAnnotatedParameterContext(data, paramIndex);
		for (Annotation parameterAnnotation : annotations) {
			AnnotatedParameterProcessor processor =
					annotatedArgumentProcessors.get(parameterAnnotation.annotationType());
			isHttpAnnotation |= processor.processArgument(context, parameterAnnotation);
		}
		return isHttpAnnotation;
	}

	private void parseProduces(MethodMetadata md, Method method, RequestMapping annotation) {
		checkAtMostOne(method, annotation.produces(), "produces");
		String[] serverProduces = annotation.produces();
		String clientAccepts = serverProduces.length == 0 ? null
				: emptyToNull(serverProduces[0]);
		if (clientAccepts != null) {
			md.template().header(ACCEPT, clientAccepts);
		}
	}

	private void parseConsumes(MethodMetadata md, Method method, RequestMapping annotation) {
		checkAtMostOne(method, annotation.consumes(), "consumes");
		String[] serverConsumes = annotation.consumes();
		String clientProduces = serverConsumes.length == 0 ? null
				: emptyToNull(serverConsumes[0]);
		if (clientProduces != null) {
			md.template().header(CONTENT_TYPE, clientProduces);
		}
	}

	private void parseHeaders(MethodMetadata md, Method method, RequestMapping annotation) {
		// TODO: only supports one header value per key
		if (annotation.headers() != null && annotation.headers().length > 0) {
			for (String header : annotation.headers()) {
				int colon = header.indexOf(':');
				md.template().header(header.substring(0, colon),
						header.substring(colon + 2));
			}
		}
	}

	private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(List<AnnotatedParameterProcessor> processors) {
		Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
		for(AnnotatedParameterProcessor processor : processors) {
			result.put(processor.getAnnotationType(), processor);
		}
		return result;
	}

	private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {

		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

		return annotatedArgumentResolvers;
	}

	private class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {

		private final MethodMetadata methodMetadata;

		private final int parameterIndex;

		public SimpleAnnotatedParameterContext(MethodMetadata methodMetadata, int parameterIndex) {
			this.methodMetadata = methodMetadata;
			this.parameterIndex = parameterIndex;
		}

		@Override
		public MethodMetadata getMethodMetadata() {
			return methodMetadata;
		}

		@Override
		public int getParameterIndex() {
			return parameterIndex;
		}

		@Override
		public void setParameterName(String name) {
			nameParam(methodMetadata, name, parameterIndex);
		}

		@Override
		public Collection<String> setTemplateParameter(String name, Collection<String> rest) {
			return addTemplatedParam(rest, name);
		}
	}

}
