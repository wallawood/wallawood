package io.gemboot.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * Resolves annotations from either {@code io.gemboot.annotations} or
 * {@code jakarta.ws.rs}, preferring the gemboot variant. This allows
 * users to use either set of annotations interchangeably.
 */
final class AnnotationSupport {

    private AnnotationSupport() {
    }

    static String findPath(AnnotatedElement element) {
        io.gemboot.annotations.Path gemboot = element.getAnnotation(io.gemboot.annotations.Path.class);
        if (gemboot != null) return gemboot.value();
        jakarta.ws.rs.Path jakarta = element.getAnnotation(jakarta.ws.rs.Path.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static boolean hasPath(AnnotatedElement element) {
        return findPath(element) != null;
    }

    static String findPathParam(Parameter param) {
        io.gemboot.annotations.PathParam gemboot = param.getAnnotation(io.gemboot.annotations.PathParam.class);
        if (gemboot != null) return gemboot.value();
        jakarta.ws.rs.PathParam jakarta = param.getAnnotation(jakarta.ws.rs.PathParam.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static String findQueryParam(Parameter param) {
        io.gemboot.annotations.QueryParam gemboot = param.getAnnotation(io.gemboot.annotations.QueryParam.class);
        if (gemboot != null) return gemboot.value();
        jakarta.ws.rs.QueryParam jakarta = param.getAnnotation(jakarta.ws.rs.QueryParam.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static String findDefaultValue(Parameter param) {
        io.gemboot.annotations.DefaultValue gemboot = param.getAnnotation(io.gemboot.annotations.DefaultValue.class);
        if (gemboot != null) return gemboot.value();
        jakarta.ws.rs.DefaultValue jakarta = param.getAnnotation(jakarta.ws.rs.DefaultValue.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static boolean hasContext(Parameter param) {
        return param.isAnnotationPresent(io.gemboot.annotations.Context.class)
                || param.isAnnotationPresent(jakarta.ws.rs.core.Context.class);
    }
}
