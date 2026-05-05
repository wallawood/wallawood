package io.gemboot.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a request interceptor discovered during classpath scanning.
 * The class must implement {@link io.gemboot.RequestInterceptor}.
 *
 * <p>Preprocessors run before any content is served — static or dynamic.
 * They can inspect or enrich the {@link io.gemboot.RequestContext} (e.g. add a
 * {@link io.gemboot.Grant}), or short-circuit the request by returning a
 * {@link io.gemboot.GeminiResponse}.
 *
 * <p>When multiple preprocessors are registered, they execute in
 * {@link #priority()} order (lower runs first).
 *
 * <p><strong>Thread safety:</strong> Instances of this class are singletons shared across
 * Reactor Netty's event loop threads. Implementations must be thread-safe — avoid mutable
 * instance fields, or protect them with appropriate synchronization.
 *
 * @see io.gemboot.RequestInterceptor
 * @see io.gemboot.RequestContext
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Preprocessor {

    /**
     * Execution priority. Lower values run first. Default is {@code 0}.
     * Preprocessors with equal priority run in undefined order.
     *
     * @return the priority value
     */
    int priority() default 0;
}
