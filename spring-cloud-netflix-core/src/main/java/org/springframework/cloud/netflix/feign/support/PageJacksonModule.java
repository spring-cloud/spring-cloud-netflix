package org.springframework.cloud.netflix.feign.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.*;

import java.util.Iterator;
import java.util.List;

/**
 * This jackson module provides support to deserialize spring {Page<T>}
 * objects.
 *
 * @author Pascal Büttiker
 */
public class PageJacksonModule extends Module {

        @Override
        public String getModuleName() {
            return "PageJacksonModule";
        }

        @Override
        public Version version() {
            return new Version(0,1,0, "", null,null);
        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(Page.class, PageMixIn.class);
        }

        @JsonDeserialize(as = SimplePageImpl.class)
        private interface PageMixIn{ }


        static class SimplePageImpl<T> implements Page<T> {

            private final Page<T> delegate;

            public SimplePageImpl(
                    @JsonProperty("content") List<T> content,
                    @JsonProperty("page")int number,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long totalElements){
                delegate = new PageImpl<>(content, new PageRequest(number, size), totalElements);
            }


            @JsonProperty
            @Override
            public int getTotalPages() {
                return delegate.getTotalPages();
            }

            @JsonProperty
            @Override
            public long getTotalElements() {
                return delegate.getTotalElements();
            }

            @JsonProperty("page")
            @Override
            public int getNumber() {
                return delegate.getNumber();
            }

            @JsonProperty
            @Override
            public int getSize() {
                return delegate.getSize();
            }

            @JsonProperty
            @Override
            public int getNumberOfElements() {
                return delegate.getNumberOfElements();
            }

            @JsonProperty
            @Override
            public List<T> getContent() {
                return delegate.getContent();
            }

            @JsonProperty
            @Override
            public boolean hasContent() {
                return delegate.hasContent();
            }

            @JsonIgnore
            @Override
            public Sort getSort() {
                return delegate.getSort();
            }

            @JsonProperty
            @Override
            public boolean isFirst() {
                return delegate.isFirst();
            }

            @JsonProperty
            @Override
            public boolean isLast() {
                return delegate.isLast();
            }

            @JsonIgnore
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @JsonIgnore
            @Override
            public boolean hasPrevious() {
                return delegate.hasPrevious();
            }

            @JsonIgnore
            @Override
            public Pageable nextPageable() {
                return delegate.nextPageable();
            }
            @JsonIgnore
            @Override
            public Pageable previousPageable() {
                return delegate.previousPageable();
            }
            @JsonIgnore
            @Override
            public <S> Page<S> map(Converter<? super T, ? extends S> converter) {
                return delegate.map(converter);
            }

            @JsonIgnore
            @Override
            public Iterator<T> iterator() {
                return delegate.iterator();
            }
        }
    }