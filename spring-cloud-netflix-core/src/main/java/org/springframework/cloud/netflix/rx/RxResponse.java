package org.springframework.cloud.netflix.rx;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;
import rx.Single;

import java.util.List;

/**
 * A convenient class allowing to wrap either the {@link Single} or {@link Observable} into a response supported by the
 * Spring MVC.
 *
 * @author Jakub Narloch
 */
public final class RxResponse {

    private RxResponse() {

    }

    /**
     * Wraps the {@link Single} into a {@link DeferredResult}.
     *
     * @param single the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<T> single(Single<T> single) {
        return new SingleDeferredResult<T>(single);
    }

    /**
     * Wraps the {@link Single} into a {@link DeferredResult}.
     *
     * @param timeout the response timeout
     * @param single the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<T> single(long timeout, Single<T> single) {
        return new SingleDeferredResult<T>(timeout, single);
    }

    /**
     * Wraps the {@link Single} into a {@link DeferredResult}.
     *
     * @param timeout the response timeout
     * @param timeoutResult the response timeout result
     * @param single the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<T> single(long timeout, Object timeoutResult, Single<T> single) {
        return new SingleDeferredResult<T>(timeout, timeoutResult, single);
    }

    /**
     * Wraps the {@link Observable} into a {@link DeferredResult}.
     *
     * @param observable the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<List<T>> observable(Observable<T> observable) {
        return new ObservableDeferredResult<T>(observable);
    }

    /**
     * Wraps the {@link Observable} into a {@link DeferredResult}.
     *
     * @param timeout the response timeout
     * @param observable the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<List<T>> observable(long timeout, Observable<T> observable) {
        return new ObservableDeferredResult<T>(timeout, observable);
    }

    /**
     * Wraps the {@link Observable} into a {@link DeferredResult}.
     *
     * @param timeout the response timeout
     * @param timeoutResult the response timeout result
     * @param observable the observable instance
     * @param <T> the result type
     * @return the deferred result
     */
    public static <T> DeferredResult<List<T>> observable(long timeout, Object timeoutResult, Observable<T> observable) {
        return new ObservableDeferredResult<T>(timeout, timeoutResult, observable);
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
        return new ObservableSseEmitter<T>(observable);
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
        return new ObservableSseEmitter<T>(mediaType, observable);
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
        return new ObservableSseEmitter<T>(timeout, mediaType, observable);
    }
}
