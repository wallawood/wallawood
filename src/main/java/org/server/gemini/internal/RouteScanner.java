package org.server.gemini.internal;

import jakarta.ws.rs.Path;
import org.server.gemini.GeminiController;
import org.server.gemini.GeminiExceptionHandler;
import org.server.gemini.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scans the classpath for classes annotated with {@link GeminiController @GeminiController}
 * and {@link GeminiExceptionHandler @GeminiExceptionHandler}, discovers their annotated
 * methods, and builds a {@link ScanResult}.
 */
public final class RouteScanner {

    private static final Logger log = LoggerFactory.getLogger(RouteScanner.class);

    private RouteScanner() {
    }

    /**
     * Scans the given base package for controllers and exception handlers.
     *
     * @param basePackage the package to scan (e.g. {@code "com.myapp"})
     * @return a {@link ScanResult} with the route registry and exception resolver
     */
    public static ScanResult scan(String basePackage) {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        Object exceptionHandler = null;

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GeminiController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(GeminiExceptionHandler.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                Object instance = clazz.getDeclaredConstructor().newInstance();

                if (clazz.isAnnotationPresent(GeminiController.class)) {
                    registerController(instance, routes);
                }

                if (clazz.isAnnotationPresent(GeminiExceptionHandler.class)) {
                    if (exceptionHandler != null) {
                        throw new IllegalStateException(
                                "Multiple @GeminiExceptionHandler classes found: "
                                        + exceptionHandler.getClass().getName() + " and " + clazz.getName());
                    }
                    exceptionHandler = instance;
                    log.info("Registered exception handler: {}", clazz.getSimpleName());
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate: " + bd.getBeanClassName(), e);
            }
        }

        log.info("Registered {} route(s) from package '{}'", routes.size(), basePackage);

        ExceptionResolver resolver = exceptionHandler != null
                ? new ExceptionResolver(exceptionHandler)
                : ExceptionResolver.none();

        return new ScanResult(new RouteRegistry(routes), resolver);
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

            if (!GeminiResponse.class.equals(method.getReturnType())) {
                throw new IllegalStateException(
                        clazz.getSimpleName() + "." + method.getName()
                                + " must return GeminiResponse, found " + method.getReturnType().getSimpleName());
            }

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
