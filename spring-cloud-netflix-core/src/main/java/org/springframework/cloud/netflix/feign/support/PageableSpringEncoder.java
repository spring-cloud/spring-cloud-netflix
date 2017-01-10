package org.springframework.cloud.netflix.feign.support;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides support for encoding spring Pageable via composition.
 *
 * @author Pascal Büttiker
 */
public class PageableSpringEncoder implements Encoder {

    private final Encoder delegate;

    /**
     * Creates a new PageableSpringEncoder with the given delegate for fallback.
     * If no delegate is provided and this encoder cant handle the request,
     * an EncodeException is thrown.
     * @param delegate The optional delegate.
     */
    public PageableSpringEncoder(Encoder delegate){
        this.delegate = delegate;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {

        if(supports(object, bodyType, template)){
            try {
                Pageable pageable = (Pageable)object;
                template.query("page", pageable.getPageNumber() + "");
                template.query("size", pageable.getPageSize() + "");

                if(pageable.getSort() != null) {
                    Collection<String> existingSorts = template.queries().get("sort");
                    List<String> sortQueries  = existingSorts != null ? new ArrayList<>(existingSorts) : new ArrayList<String>();
                    for (Sort.Order order : pageable.getSort()) {
                        sortQueries.add(order.getProperty() + "," + order.getDirection());
                    }
                    template.query("sort", sortQueries);
                }
            }catch (Exception e){
                throw new EncodeException("Failed to encode the given Pageable!", e);
            }
        }else{
            if(delegate != null){
                delegate.encode(object, bodyType, template);
            }else{
                throw new EncodeException("PageableSpringEncoder does not support the given object "+object.getClass()+" and no delegate was provided for fallback!");
            }
        }
    }

    public boolean supports(Object object, Type bodyType, RequestTemplate template){
        return object instanceof Pageable;
    }
}
