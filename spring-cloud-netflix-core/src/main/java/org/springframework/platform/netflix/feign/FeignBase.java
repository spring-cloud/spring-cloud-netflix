package org.springframework.platform.netflix.feign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Created by sgibb on 6/27/14.
 */
public class FeignBase {
    public static final boolean romePresent =
            ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", FeignBase.class.getClassLoader());

    public static final boolean jaxb2Present =
            ClassUtils.isPresent("javax.xml.bind.Binder", FeignBase.class.getClassLoader());

    public static final boolean jackson2Present =
            ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", FeignBase.class.getClassLoader()) &&
                    ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", FeignBase.class.getClassLoader());

    public static final boolean jacksonPresent =
            ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", FeignBase.class.getClassLoader()) &&
                    ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", FeignBase.class.getClassLoader());

    protected final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

    public FeignBase() {
        addDefaultConverters(messageConverters);
    }

    /**
     * Create a new instance of the {@link SpringDecoder} using the given list of
     * {@link HttpMessageConverter} to use
     * @param messageConverters the list of {@link HttpMessageConverter} to use
     */
    public FeignBase(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
        this.messageConverters.addAll(messageConverters);
    }

    @SuppressWarnings("deprecation")
	protected void addDefaultConverters(List<HttpMessageConverter<?>> messageConverters) {
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new SourceHttpMessageConverter<Source>());
        messageConverters.add(new AllEncompassingFormHttpMessageConverter());

        if (romePresent) {
            messageConverters.add(new AtomFeedHttpMessageConverter());
            messageConverters.add(new RssChannelHttpMessageConverter());
        }
        if (jaxb2Present) {
            messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        }
        if (jackson2Present) {
            messageConverters.add(new MappingJackson2HttpMessageConverter());
        }
        else if (jacksonPresent) {
            messageConverters.add(new org.springframework.http.converter.json.MappingJacksonHttpMessageConverter());
        }
    }

    /**
     * Set the message body converters to use.
     * <p>These converters are used to convert from and to HTTP requests and responses.
     */
    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
        this.messageConverters.clear();
        this.messageConverters.addAll(messageConverters);
    }

    public List<HttpMessageConverter<?>> getMessageConverters() {
        return messageConverters;
    }

    protected HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return httpHeaders;
    }

}
