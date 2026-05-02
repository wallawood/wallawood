package io.gemboot.internal;

import io.gemboot.GeminiResponse;
import io.gemboot.Authorization;
import io.gemboot.Grant;
import io.gemboot.RequestContext;
import io.gemboot.annotations.Authorize;
import io.gemboot.annotations.QueryString;
import io.gemboot.annotations.RequireCertificate;
import io.gemboot.annotations.RequireInput;
import io.gemboot.annotations.RequireSensitiveInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves handler method parameters and invokes the handler. Pre-invocation
 * checks run in order: {@link RequireCertificate @RequireCertificate},
 * {@link Authorize @Authorize}, {@link RequireInput @RequireInput},
 * {@link RequireSensitiveInput @RequireSensitiveInput}.
 *
 * <p>Both {@code io.gemboot.annotations} and {@code jakarta.ws.rs} annotation
 * variants are accepted for parameter binding.
 *
 * <p>Handler methods must return {@link GeminiResponse}.
 */
public final class HandlerInvoker {

    private static final Logger log = LoggerFactory.getLogger(HandlerInvoker.class);

    private HandlerInvoker() {
    }

    /**
     * Invokes the matched handler method, resolving parameters from the
     * {@link RequestContext}.
     *
     * @param matched the matched route with path variables
     * @param requestContext the request context containing URI, client cert, grant, etc.
     * @param exceptionResolver the resolver for mapping handler exceptions to responses
     * @return the response from the handler, or an error/auth response
     */
    public static GeminiResponse invoke(RouteRegistry.MatchedRoute matched, RequestContext requestContext,
                                         ExceptionResolver exceptionResolver) {
        HandlerMethod handler = matched.handler();
        Method method = handler.method();

        X509Certificate clientCert = requestContext.get(X509Certificate.class);
        URI requestUri = requestContext.get(URI.class);

        if (requiresCertificate(handler) && clientCert == null) {
            String message = getCertificateMessage(handler);
            return message.isEmpty()
                    ? GeminiResponse.clientCertificateRequired()
                    : GeminiResponse.clientCertificateRequired(message);
        }

        GeminiResponse authResponse = checkAuthorize(handler, requestContext);
        if (authResponse != null) {
            return authResponse;
        }

        String query = requestUri.getRawQuery();
        boolean queryMissing = query == null || query.isEmpty();

        RequireInput requireInput = method.getAnnotation(RequireInput.class);
        if (requireInput != null && queryMissing) {
            return GeminiResponse.input(requireInput.value());
        }

        RequireSensitiveInput requireSensitive = method.getAnnotation(RequireSensitiveInput.class);
        if (requireSensitive != null && queryMissing) {
            return GeminiResponse.sensitiveInput(requireSensitive.value());
        }

        try {
            Object[] args = resolveArgs(method, matched.pathVariables(), requestContext);
            return (GeminiResponse) method.invoke(handler.controller(), args);
        } catch (IllegalArgumentException e) {
            log.debug("Bad request to {}.{}: {}",
                    handler.controller().getClass().getSimpleName(),
                    method.getName(), e.getMessage());
            return GeminiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Handler {}.{} threw an exception",
                    handler.controller().getClass().getSimpleName(),
                    method.getName(), cause);

            GeminiResponse resolved = exceptionResolver.resolve(cause);
            if (resolved != null) {
                return resolved;
            }
            return GeminiResponse.temporaryFailure("An unexpected error occurred");
        }
    }

    private static boolean requiresCertificate(HandlerMethod handler) {
        return handler.method().isAnnotationPresent(RequireCertificate.class)
                || handler.controller().getClass().isAnnotationPresent(RequireCertificate.class);
    }

    private static String getCertificateMessage(HandlerMethod handler) {
        RequireCertificate methodAnnotation = handler.method().getAnnotation(RequireCertificate.class);
        if (methodAnnotation != null && !methodAnnotation.value().isEmpty()) {
            return methodAnnotation.value();
        }
        RequireCertificate classAnnotation = handler.controller().getClass().getAnnotation(RequireCertificate.class);
        if (classAnnotation != null && !classAnnotation.value().isEmpty()) {
            return classAnnotation.value();
        }
        return "";
    }

    private static GeminiResponse checkAuthorize(HandlerMethod handler, RequestContext ctx) {
        Authorize auth = handler.method().getAnnotation(Authorize.class);
        if (auth == null) {
            auth = handler.controller().getClass().getAnnotation(Authorize.class);
        }
        if (auth == null) {
            return null;
        }

        X509Certificate cert = ctx.get(X509Certificate.class);
        if (cert == null) {
            return GeminiResponse.clientCertificateRequired(auth.message());
        }

        Grant grant = ctx.get(Grant.class);
        Authorization authz = fromAnnotation(auth);
        if (!authz.check(grant)) {
            return GeminiResponse.certificateNotAuthorized(auth.message());
        }

        return null;
    }

    private static Authorization fromAnnotation(Authorize auth) {
        return new Authorization(auth.level(), auth.scopes());
    }

    private static Object[] resolveArgs(Method method, Map<String, String> pathVars, RequestContext requestContext) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        URI requestUri = requestContext.get(URI.class);
        Map<String, String> queryParams = parseQuery(requestUri);

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];

            String pathParamName = AnnotationSupport.findPathParam(param);
            if (pathParamName != null) {
                String value = pathVars.get(pathParamName);
                String resolved = applyDefault(value, param);
                args[i] = convert(resolved, param.getType());
                continue;
            }

            String queryParamName = AnnotationSupport.findQueryParam(param);
            if (queryParamName != null) {
                String value = queryParams.get(queryParamName);
                String resolved = applyDefault(value, param);
                args[i] = convert(resolved, param.getType());
                continue;
            }

            if (param.isAnnotationPresent(QueryString.class)) {
                String raw = requestUri.getRawQuery();
                args[i] = raw != null ? decode(raw) : applyDefault(null, param);
                continue;
            }

            if (AnnotationSupport.hasContext(param)) {
                boolean found = false;
                for (var entry : requestContext.entrySet()) {
                    if (param.getType().isAssignableFrom(entry.getKey())) {
                        args[i] = entry.getValue();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    args[i] = null;
                }
                continue;
            }

            args[i] = null;
        }

        return args;
    }

    private static Object convert(String value, Class<?> type) {
        if (value == null) {
            return type.isPrimitive() ? defaultPrimitive(type) : null;
        }
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for type " + type.getSimpleName());
        }
    }

    private static Object defaultPrimitive(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        if (type == double.class) return 0.0;
        return null;
    }

    private static String applyDefault(String value, Parameter param) {
        if (value != null) return value;
        return AnnotationSupport.findDefaultValue(param);
    }

    private static Map<String, String> parseQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return params;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
