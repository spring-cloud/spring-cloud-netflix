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

package org.springframework.cloud.netflix.zuul.metrics;

import com.netflix.zuul.monitoring.Tracer;
import com.netflix.zuul.monitoring.TracerFactory;

/**
 * A time based monitoring factory that does nothing.
 *
 * @author Anastasiia Smirnova
 */
public class EmptyTracerFactory extends TracerFactory {

	private final EmptyTracer emptyTracer = new EmptyTracer();

	@Override
	public Tracer startMicroTracer(String name) {
		return emptyTracer;
	}

	private static final class EmptyTracer implements Tracer {
		@Override
		public void setName(String name) {
		}

		@Override
		public void stopAndLog() {
		}
	}
}
