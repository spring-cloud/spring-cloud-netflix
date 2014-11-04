package org.springframework.cloud.netflix.zuul;

/**
 * @author Spencer Gibb
 */
public interface ZuulProperties {
    public String getMapping();
    public boolean isStripMapping();
    public String getRoutePrefix();
    public boolean isAddProxyHeaders();
}
