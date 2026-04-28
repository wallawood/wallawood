package org.server.gemini.internal;

import jakarta.ws.rs.Path;
import org.server.gemini.GeminiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scans the classpath for classes annotated with {@link GeminiController @GeminiController},
 * discovers their {@link Path @Path}-annotated methods, and builds a {@link RouteRegistry}.
 */
public final class RouteScanner {

    private static final Logger log = LoggerFactory.getLogger(RouteScanner.class);

    private RouteScanner() {
    }

    /**
     * Scans the given base package for {@link GeminiController @GeminiController} classes
     * and builds a route registry from their {@link Path @Path}-annotated methods.
     *
     * @param basePackage the package to scan (e.g. {@code "com.myapp"})
     * @return a populated {@link RouteRegistry}
     */
    public static RouteRegistry scan(String basePackage) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GeminiController.class));

        Map<String, HandlerMethod> routes = new LinkedHashMap<>();

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                Object instance = clazz.getDeclaredConstructor().newInstance();
                registerController(instance, routes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate controller: " + bd.getBeanClassName(), e);
            }
        }

        log.info("Registered {} route(s) from package '{}'", routes.size(), basePackage);
        return new RouteRegistry(routes);
    }

    /**
     * Registers routes from a single controller instance.
     */
    static void registerController(Object controller, Map<String, HandlerMethod> routes) {
        Class<?> clazz = controller.getClass();

        if (!clazz.isAnnotationPresent(GeminiController.class)) {
            throw new IllegalArgumentException(clazz.getName() + " is not annotated with @GeminiController");
        }

        String classPath = "";
        Path classPathAnnotation = clazz.getAnnotation(Path.class);
        if (classPathAnnotation != null) {
            classPath = normalize(classPathAnnotation.value());
        }

        for (Method method : clazz.getDeclaredMethods()) {
            Path methodPath = method.getAnnotation(Path.class);
            if (methodPath == null) {
                continue;
            }

            String fullPath = classPath + normalize(methodPath.value());
            if (fullPath.isEmpty()) {
                fullPath = "/";
            }

            log.debug("Mapping {} -> {}.{}", fullPath, clazz.getSimpleName(), method.getName());

            if (routes.containsKey(fullPath)) {
                throw new IllegalStateException("Duplicate route: " + fullPath);
            }

            routes.put(fullPath, new HandlerMethod(controller, method));
        }
    }

    private static String normalize(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
