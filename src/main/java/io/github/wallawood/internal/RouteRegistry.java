package io.github.wallawood.internal;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registry mapping URI path patterns to handler methods. Uses Spring's
 * {@link PathPatternParser} for pattern matching with support for path
 * variables and catch-all segments.
 */
public final class RouteRegistry {

    private final Map<PathPattern, HandlerMethod> routes;
    private final PathPatternParser parser;

    RouteRegistry(Map<String, HandlerMethod> rawRoutes) {
        this.parser = new PathPatternParser();
        Map<PathPattern, HandlerMethod> parsed = new LinkedHashMap<>();
        for (var entry : rawRoutes.entrySet()) {
            parsed.put(parser.parse(entry.getKey()), entry.getValue());
        }
        var sorted = new LinkedHashMap<PathPattern, HandlerMethod>();
        parsed.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        this.routes = Collections.unmodifiableMap(sorted);
    }

    /**
     * Finds the handler method matching the given request path.
     *
     * @param path the request URI path (e.g. {@code "/users/42"})
     * @return the matching route, or {@code null} if no route matches
     */
    public MatchedRoute match(String path) {
        var pathContainer = PathContainer.parsePath(path);
        for (var entry : routes.entrySet()) {
            var info = entry.getKey().matchAndExtract(pathContainer);
            if (info != null) {
                return new MatchedRoute(entry.getValue(), info.getUriVariables());
            }
        }
        return null;
    }

    /**
     * Returns all registered routes.
     *
     * @return an unmodifiable map of path patterns to handler methods
     */
    public Map<PathPattern, HandlerMethod> routes() {
        return routes;
    }

    /**
     * A matched route with extracted path variables.
     *
     * @param handler the handler method that matched the request path
     * @param pathVariables the path variables extracted from the URI (e.g. {@code {id} → "42"})
     */
    public record MatchedRoute(HandlerMethod handler, Map<String, String> pathVariables) {
    }
}
