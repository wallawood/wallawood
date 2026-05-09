package io.github.wallawood.internal;

import io.github.wallawood.RequestInterceptor;

import java.util.List;

/**
 * The result of classpath scanning: a route registry, an optional exception resolver,
 * and any discovered {@link io.github.wallawood.annotations.Preprocessor @Preprocessor} interceptors.
 *
 * @param routeRegistry the registry of URI patterns to handler methods
 * @param exceptionResolver the resolver for mapping exceptions to Gemini responses
 * @param interceptors the ordered list of request interceptors
 */
public record ScanResult(RouteRegistry routeRegistry, ExceptionResolver exceptionResolver,
                         List<RequestInterceptor> interceptors) {
}
