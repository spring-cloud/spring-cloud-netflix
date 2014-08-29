package org.springframework.cloud.netflix.zuul;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;

import com.netflix.zuul.context.ContextLifecycleFilter;
import com.netflix.zuul.http.ZuulServlet;

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
    
    @Autowired(required=false)
    private TraceRepository traces;

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
        return new Routes();
    }

    @Bean
    FilterInitializer zuulFilterInitializer() {
        return new FilterInitializer();
    }

    // pre filters
    @Bean
    public DebugFilter debugFilter() {
        return new DebugFilter();
    }

    @Bean
    public PreDecorationFilter preDecorationFilter() {
        return new PreDecorationFilter();
    }

    // route filters
    @Bean
    public RibbonRoutingFilter ribbonRoutingFilter() {
        RibbonRoutingFilter filter = new RibbonRoutingFilter();
        if (traces!=null) {
        	filter.setTraces(traces);
        }
		return filter;
    }

    // post filters
    @Bean
    public SendResponseFilter sendResponseFilter() {
        return new SendResponseFilter();
    }

}
