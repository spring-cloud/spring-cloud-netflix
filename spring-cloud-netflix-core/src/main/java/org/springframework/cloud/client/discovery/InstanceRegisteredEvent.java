package org.springframework.cloud.client.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
public class InstanceRegisteredEvent extends ApplicationEvent {
    private Object config;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the component that published the event (never {@code null})
     */
    public InstanceRegisteredEvent(Object source, Object config) {
        super(source);
        this.config = config;
    }

    public Object getConfig() {
        return config;
    }
}
