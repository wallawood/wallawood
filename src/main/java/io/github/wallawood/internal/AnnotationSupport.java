package io.github.wallawood.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * Resolves annotations from either {@code io.github.wallawood.annotations} or
 * {@code jakarta.ws.rs}, preferring the wallawood variant. This allows
 * users to use either set of annotations interchangeably.
 */
final class AnnotationSupport {

    private AnnotationSupport() {
    }

    static String findPath(AnnotatedElement element) {
        io.github.wallawood.annotations.Path wallawood = element.getAnnotation(io.github.wallawood.annotations.Path.class);
        if (wallawood != null) return wallawood.value();
        jakarta.ws.rs.Path jakarta = element.getAnnotation(jakarta.ws.rs.Path.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static boolean hasPath(AnnotatedElement element) {
        return findPath(element) != null;
    }

    static String findPathParam(Parameter param) {
        io.github.wallawood.annotations.PathParam wallawood = param.getAnnotation(io.github.wallawood.annotations.PathParam.class);
        if (wallawood != null) return wallawood.value();
        jakarta.ws.rs.PathParam jakarta = param.getAnnotation(jakarta.ws.rs.PathParam.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static String findQueryParam(Parameter param) {
        io.github.wallawood.annotations.QueryParam wallawood = param.getAnnotation(io.github.wallawood.annotations.QueryParam.class);
        if (wallawood != null) return wallawood.value();
        jakarta.ws.rs.QueryParam jakarta = param.getAnnotation(jakarta.ws.rs.QueryParam.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static String findDefaultValue(Parameter param) {
        io.github.wallawood.annotations.DefaultValue wallawood = param.getAnnotation(io.github.wallawood.annotations.DefaultValue.class);
        if (wallawood != null) return wallawood.value();
        jakarta.ws.rs.DefaultValue jakarta = param.getAnnotation(jakarta.ws.rs.DefaultValue.class);
        return jakarta != null ? jakarta.value() : null;
    }

    static boolean hasContext(Parameter param) {
        return param.isAnnotationPresent(io.github.wallawood.annotations.Context.class)
                || param.isAnnotationPresent(jakarta.ws.rs.core.Context.class);
    }
}
