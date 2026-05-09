package io.github.wallawood.internal;

import io.github.wallawood.GeminiResponse;
import io.github.wallawood.annotations.GeminiExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves exceptions to {@link GeminiResponse} using a user-defined
 * {@link GeminiExceptionHandler @GeminiExceptionHandler} class.
 * Methods are matched by exception type — the most specific match wins.
 */
public final class ExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(ExceptionResolver.class);
    private static final GeminiResponse DEFAULT_RESPONSE =
            GeminiResponse.temporaryFailure("An unexpected error occurred");

    private final Object handler;
    private final Map<Class<? extends Throwable>, Method> methods;

    /**
     * Creates a resolver from the given exception handler instance.
     * Validates that all methods accept a single Throwable parameter and return GeminiResponse.
     *
     * @param handler the {@link GeminiExceptionHandler @GeminiExceptionHandler} instance
     */
    public ExceptionResolver(Object handler) {
        this.handler = handler;
        this.methods = new LinkedHashMap<>();

        for (Method m : handler.getClass().getDeclaredMethods()) {
            if (m.getReturnType() != GeminiResponse.class) {
                continue;
            }
            if (m.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = m.getParameterTypes()[0];
            if (!Throwable.class.isAssignableFrom(paramType)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exType = (Class<? extends Throwable>) paramType;
            methods.put(exType, m);
        }
    }

    /**
     * Resolves the given exception to a GeminiResponse. Finds the most specific
     * matching handler method by walking the exception's class hierarchy.
     *
     * @param ex the exception thrown by a handler method
     * @return the resolved response, or {@code null} if no handler matches
     */
    public GeminiResponse resolve(Throwable ex) {
        Class<?> exClass = ex.getClass();
        while (exClass != null) {
            Method m = methods.get(exClass);
            if (m != null) {
                try {
                    return (GeminiResponse) m.invoke(handler, ex);
                } catch (Exception e) {
                    log.error("Exception handler threw an exception", e);
                    return DEFAULT_RESPONSE;
                }
            }
            exClass = exClass.getSuperclass();
        }
        return null;
    }

    /**
     * Returns a no-op resolver that always returns null (no match).
     *
     * @return an empty resolver
     */
    public static ExceptionResolver none() {
        return new ExceptionResolver(new Object());
    }
}
