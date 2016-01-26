package org.springframework.cloud.netflix.rx;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import rx.Observable;

/**
 * A convenient class allowing to wrap either the {@link Observable} into a response supported by the
 * Spring MVC.
 *
 * @author Jakub Narloch
 */
public final class RxResponse {

    private RxResponse() {

    }

    /**
     * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
     * as server side event.
     *
     * @param observable the observable instance
     * @param <T> the result type
     * @return the sse emitter
     */
    public static <T> SseEmitter sse(Observable<T> observable) {
        return new ObservableSseEmitter<>(observable);
    }

    /**
     * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
     * as server side event.
     *
     * @param mediaType the media type of produced entry
     * @param observable the observable instance
     * @param <T> the result type
     * @return the sse emitter
     */
    public static <T> SseEmitter sse(MediaType mediaType, Observable<T> observable) {
        return new ObservableSseEmitter<>(mediaType, observable);
    }

    /**
     * Wraps the {@link Observable} into a {@link SseEmitter}. Every value produced by the observable will be emitted
     * as server side event.
     *
     * @param timeout the response timeout
     * @param mediaType the media type of produced entry
     * @param observable the observable instance
     * @param <T> the result type
     * @return the sse emitter
     */
    public static <T> SseEmitter sse(long timeout, MediaType mediaType, Observable<T> observable) {
        return new ObservableSseEmitter<>(timeout, mediaType, observable);
    }
}
