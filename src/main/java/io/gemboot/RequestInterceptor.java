package io.gemboot;

import java.util.Optional;

/**
 * Intercepts requests before routing. Implementations are discovered via
 * {@link io.gemboot.annotations.Preprocessor @Preprocessor} during classpath
 * scanning and run in {@link io.gemboot.annotations.Preprocessor#priority() priority}
 * order (lower values run first).
 *
 * <p>Interceptors can:
 * <ul>
 *   <li><b>Enrich the context</b> — add a {@link Grant}, a user object, or any
 *       custom data to the {@link RequestContext} for downstream handlers.</li>
 *   <li><b>Short-circuit the request</b> — return a {@link GeminiResponse} to
 *       stop the pipeline (e.g. deny access, redirect).</li>
 *   <li><b>Pass through</b> — return {@link Optional#empty()} to let the
 *       request continue to the next interceptor or route handler.</li>
 * </ul>
 *
 * <p>Interceptors run before both static file serving and dynamic route matching,
 * making them suitable for authentication, authorization, rate limiting, and
 * request logging.
 *
 * <p>Use {@link Authorization} to check grants with the same logic the framework
 * uses for {@link io.gemboot.annotations.Authorize @Authorize}:
 *
 * <pre>{@code
 * @Preprocessor(priority = 10)
 * public class AdminGuard implements RequestInterceptor {
 *
 *     private static final Authorization ADMIN = Authorization.level(3);
 *
 *     @Override
 *     public Optional<GeminiResponse> intercept(RequestContext context) {
 *         URI uri = context.get(URI.class);
 *         if (uri != null && uri.getPath().startsWith("/admin")) {
 *             Grant grant = context.get(Grant.class);
 *             if (!ADMIN.check(grant)) {
 *                 return Optional.of(GeminiResponse.certificateNotAuthorized("Admins only."));
 *             }
 *         }
 *         return Optional.empty();
 *     }
 * }
 * }</pre>
 *
 * @see io.gemboot.annotations.Preprocessor
 * @see RequestContext
 * @see Authorization
 */
public interface RequestInterceptor {

    /**
     * Intercepts a request before routing. Return a {@link GeminiResponse} to
     * short-circuit, or {@link Optional#empty()} to continue the pipeline.
     *
     * @param context the request context, writable for enrichment
     * @return a response to send immediately, or empty to continue
     */
    Optional<GeminiResponse> intercept(RequestContext context);
}
