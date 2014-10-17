package org.springframework.cloud.netflix.ribbon;

/**
 * Allows different service discovery implementations to configure ribbon prior to usage.
 * TODO: this could potentially be done via AOP
 * @author Spencer Gibb
 */
public interface RibbonClientPreprocessor {
    public void preprocess(String serviceId);
}
