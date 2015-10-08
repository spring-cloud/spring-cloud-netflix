/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.metrics.servo;

import org.springframework.cloud.netflix.metrics.MetricsClientHttpRequestInterceptor;
import org.springframework.cloud.netflix.metrics.MetricsHandlerInterceptor;
import org.springframework.cloud.netflix.metrics.MetricsInterceptorConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation to enable Servo metrics collection. This should only be used if you require JDK 7.  Otherwise, favor @EnableSpectator.
 *
 * @author Jon Schneider
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ ServoMetricsAutoConfiguration.class, MetricsInterceptorConfiguration.class })
public @interface EnableServo {
}
