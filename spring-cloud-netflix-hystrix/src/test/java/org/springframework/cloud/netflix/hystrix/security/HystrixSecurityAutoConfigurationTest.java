package org.springframework.cloud.netflix.hystrix.security;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * @author : ailin.zhou
 */
public class HystrixSecurityAutoConfigurationTest {

    @Test
    public void testInit() throws NoSuchFieldException, IllegalAccessException {

        //save test context
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
                .getEventNotifier();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
                .getMetricsPublisher();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
                .getPropertiesStrategy();
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance()
                .getCommandExecutionHook();
        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();

        //test
        testForMultiConcurrentStrategy();

        //recover test context
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerConcurrencyStrategy(concurrencyStrategy);
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);


    }

    private void testForMultiConcurrentStrategy() throws IllegalAccessException, NoSuchFieldException {
        HystrixSecurityAutoConfiguration securityStrategy = new HystrixSecurityAutoConfiguration();

        //1.existingConcurrencyStrategy is null, registeredStrategy is default
        HystrixPlugins.reset();
        securityStrategy.init();
        //result is default
        assertEquals(HystrixConcurrencyStrategyDefault.getInstance(), getOriginalInSecurityConcurrencyStrategy());

        //2.existingConcurrencyStrategy is null, registered strategy is customized
        HystrixPlugins.reset();
        HystrixConcurrencyStrategy customized = new HystrixConcurrencyStrategy() {
        };
        HystrixPlugins.getInstance().registerConcurrencyStrategy(customized);
        securityStrategy.init();
        //result is customized
        assertEquals(customized, getOriginalInSecurityConcurrencyStrategy());

        //3.existingConcurrencyStrategy is not null, registeredStrategy is  default.
        HystrixPlugins.reset();
        HystrixConcurrencyStrategy existingConcurrencyStrategy = new HystrixConcurrencyStrategy() {
        };
        FieldSetter.setField(securityStrategy, securityStrategy.getClass().getDeclaredField("existingConcurrencyStrategy"), existingConcurrencyStrategy);
        securityStrategy.init();
        //result is existingConcurrencyStrategy
        assertEquals(existingConcurrencyStrategy, getOriginalInSecurityConcurrencyStrategy());

        //4.existingConcurrencyStrategy is not null, registeredStrategy is  customized.
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerConcurrencyStrategy(customized);
        FieldSetter.setField(securityStrategy, securityStrategy.getClass().getDeclaredField("existingConcurrencyStrategy"), existingConcurrencyStrategy);
        try {
            securityStrategy.init();
        } catch (IllegalStateException e) {
            // this is the correct case
            assertEquals("Multiple HystrixConcurrencyStrategy detected.", e.getMessage());
        }
    }

    private HystrixConcurrencyStrategy getOriginalInSecurityConcurrencyStrategy() throws IllegalAccessException, NoSuchFieldException {
        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
        Field existingConcurrencyStrategy = concurrencyStrategy.getClass().getDeclaredField("existingConcurrencyStrategy");
        existingConcurrencyStrategy.setAccessible(true);
        HystrixConcurrencyStrategy strategyInSecurityStrategy = (HystrixConcurrencyStrategy) existingConcurrencyStrategy.get(concurrencyStrategy);
        return strategyInSecurityStrategy;
    }

}