package org.springframework.platform.netflix.zuul;

import com.netflix.zuul.context.ContextLifecycleFilter;
import com.netflix.zuul.http.ZuulServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.platform.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.platform.netflix.zuul.filters.post.StatsFilter;
import org.springframework.platform.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.platform.netflix.zuul.filters.pre.DebugRequestFilter;
import org.springframework.platform.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.platform.netflix.zuul.filters.route.RibbonRoutingFilter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties(ZuulProxyProperties.class)
@ConditionalOnClass(ZuulServlet.class)
@ConditionalOnExpression("${zuul.proxy.enabled:true}")
public class ZuulProxyConfiguration {

    @Autowired
    private ZuulProxyProperties props;

    @Bean
    public FilterRegistrationBean contextLifecycleFilter() {
        Collection<String> urlPatterns = new ArrayList<>();
        urlPatterns.add(props.getMapping()+"/*");

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new ContextLifecycleFilter());
        filterRegistrationBean.setUrlPatterns(urlPatterns);

        return filterRegistrationBean;
    }

    @Bean
    public ServletRegistrationBean zuulServlet() {
        return new ServletRegistrationBean(new ZuulServlet(), props.getMapping()+"/*");
    }

    @Bean
    Routes routes() {
        return new Routes("zuul.proxy.route.");
    }

    @Bean
    FilterIntializer filterIntializer() {
        return new FilterIntializer();
    }

    // pre filters
    @Bean
    public DebugFilter debugFilter() {
        return new DebugFilter();
    }

    @Bean
    public DebugRequestFilter debugRequestFilter() {
        return new DebugRequestFilter();
    }

    @Bean
    public PreDecorationFilter preDecorationFilter() {
        return new PreDecorationFilter();
    }

    // route filters
    @Bean
    public RibbonRoutingFilter ribbonRoutingFilter() {
        return new RibbonRoutingFilter();
    }

    // post filters
    @Bean
    public SendResponseFilter sendResponseFilter() {
        return new SendResponseFilter();
    }

    @Bean
    public StatsFilter statsFilter() {
        return new StatsFilter();
    }

}
