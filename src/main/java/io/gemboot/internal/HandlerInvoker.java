package io.gemboot.internal;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import io.gemboot.GeminiResponse;
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
 * Resolves handler method parameters and invokes the handler. Supports
 * {@link PathParam @PathParam}, {@link QueryParam @QueryParam},
 * {@link DefaultValue @DefaultValue}, {@link Context @Context},
 * and {@link RequireCertificate @RequireCertificate}.
 *
 * <p>Handler methods must return {@link GeminiResponse}. Any other return type
 * is treated as an error.
 */
public final class HandlerInvoker {

    private static final Logger log = LoggerFactory.getLogger(HandlerInvoker.class);

    private HandlerInvoker() {
    }

    /**
     * Invokes the matched handler method, resolving parameters from the request.
     *
     * @param matched the matched route with path variables
     * @param requestUri the full request URI
     * @param clientCert the client's TLS certificate, or {@code null} if not provided
     * @return the response from the handler, or an error response
     */
    public static GeminiResponse invoke(RouteRegistry.MatchedRoute matched, URI requestUri,
                                         X509Certificate clientCert, ExceptionResolver exceptionResolver) {
        HandlerMethod handler = matched.handler();
        Method method = handler.method();

        if (requiresCertificate(handler) && clientCert == null) {
            String message = getCertificateMessage(handler);
            return message.isEmpty()
                    ? GeminiResponse.clientCertificateRequired()
                    : GeminiResponse.clientCertificateRequired(message);
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
            Object[] args = resolveArgs(method, matched.pathVariables(), requestUri, clientCert);
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

    private static Object[] resolveArgs(Method method, Map<String, String> pathVars, URI requestUri, X509Certificate clientCert) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        Map<String, String> queryParams = parseQuery(requestUri);

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];

            PathParam pathParam = param.getAnnotation(PathParam.class);
            if (pathParam != null) {
                String value = pathVars.get(pathParam.value());
                String resolved = applyDefault(value, param);
                args[i] = convert(resolved, param.getType());
                continue;
            }

            QueryParam queryParam = param.getAnnotation(QueryParam.class);
            if (queryParam != null) {
                String value = queryParams.get(queryParam.value());
                String resolved = applyDefault(value, param);
                args[i] = convert(resolved, param.getType());
                continue;
            }

            if (param.isAnnotationPresent(QueryString.class)) {
                String raw = requestUri.getRawQuery();
                args[i] = raw != null ? decode(raw) : applyDefault(null, param);
                continue;
            }

            if (param.isAnnotationPresent(Context.class)) {
                if (param.getType().isAssignableFrom(URI.class)) {
                    args[i] = requestUri;
                } else if (param.getType().isAssignableFrom(X509Certificate.class)) {
                    args[i] = clientCert;
                } else {
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
            if (type == String.class) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            }
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
        if (value != null) {
            return value;
        }
        DefaultValue defaultValue = param.getAnnotation(DefaultValue.class);
        return defaultValue != null ? defaultValue.value() : null;
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
