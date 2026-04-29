package io.gemboot.internal;

/**
 * The result of classpath scanning: a route registry and an optional exception resolver.
 */
public record ScanResult(RouteRegistry routeRegistry, ExceptionResolver exceptionResolver) {
}
