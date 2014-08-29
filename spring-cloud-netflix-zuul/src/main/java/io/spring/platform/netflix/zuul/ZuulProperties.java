package io.spring.platform.netflix.zuul;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * User: spencergibb
 * Date: 5/2/14
 * Time: 9:20 PM
 */
@ConfigurationProperties("zuul")
public class ZuulProperties {
    private String filterRoot;
    private long cacheRefresh = MINUTES.toMillis(5L);

    public String getFilterRoot() {
        return filterRoot;
    }

    public void setFilterRoot(String filterRoot) {
        this.filterRoot = filterRoot;
    }

    public long getCacheRefresh() {
        return cacheRefresh;
    }

    public void setCacheRefresh(long cacheRefresh) {
        this.cacheRefresh = cacheRefresh;
    }
}
