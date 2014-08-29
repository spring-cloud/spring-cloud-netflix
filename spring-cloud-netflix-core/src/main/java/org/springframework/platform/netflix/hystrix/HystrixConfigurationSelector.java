package org.springframework.platform.netflix.hystrix;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.platform.netflix.hystrix.annotations.EnableHystrix;

/**
 * Created by sgibb on 6/19/14.
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
