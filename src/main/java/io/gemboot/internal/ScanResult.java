package io.gemboot.internal;

/**
 * The result of classpath scanning: a route registry and an optional exception resolver.
 *
 * @param routeRegistry the registry of URI patterns to handler methods
 * @param exceptionResolver the resolver for mapping exceptions to Gemini responses
 */
public record ScanResult(RouteRegistry routeRegistry, ExceptionResolver exceptionResolver) {
}
