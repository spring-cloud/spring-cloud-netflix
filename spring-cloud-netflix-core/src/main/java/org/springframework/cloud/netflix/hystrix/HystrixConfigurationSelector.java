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
package org.springframework.cloud.netflix.hystrix;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;

/**
 * @author Spencer Gibb
 */
public class HystrixConfigurationSelector extends AdviceModeImportSelector<EnableHystrix> {
    
	/**
     * The name of the AspectJ transaction management @{@code Configuration} class.
     */
    private static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
            "org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration";

    @Override
    protected String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return new String[]{AutoProxyRegistrar.class.getName(), HystrixConfiguration.class.getName()};
            case ASPECTJ:
                return new String[]{TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
            default:
                return null;
        }
    }
}
