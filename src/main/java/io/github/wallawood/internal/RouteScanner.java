package io.github.wallawood.internal;

import io.github.wallawood.RequestInterceptor;
import io.github.wallawood.annotations.Component;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.annotations.GeminiExceptionHandler;
import io.github.wallawood.annotations.Preprocessor;
import io.github.wallawood.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the classpath for classes annotated with {@link GeminiController @GeminiController},
 * {@link GeminiExceptionHandler @GeminiExceptionHandler}, and
 * {@link Preprocessor @Preprocessor}, discovers their annotated methods, and builds
 * a {@link ScanResult}.
 *
 * <p>Supports constructor injection: if a scanned class has no no-arg constructor,
 * the scanner resolves constructor parameters from other scanned classes. All
 * parameter types must be satisfiable by another scanned component or instantiation
 * fails fast.
 */
public final class RouteScanner {

    private static final Logger log = LoggerFactory.getLogger(RouteScanner.class);

    private RouteScanner() {
    }

    /**
     * Scans the given base package for controllers, exception handlers, and preprocessors.
     *
     * @param basePackage the package to scan (e.g. {@code "com.myapp"})
     * @return a {@link ScanResult} with the route registry, exception resolver, and interceptors
     */
    public static ScanResult scan(String basePackage) {
        AccessLog.log("Scanning '{}' for annotated classes...", basePackage);

        List<Class<?>> discovered = discoverClasses(basePackage);
        Map<Class<?>, Object> components = instantiateAll(discovered);

        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        Object exceptionHandler = null;
        List<RequestInterceptor> interceptors = new ArrayList<>();

        for (Object instance : components.values()) {
            Class<?> clazz = instance.getClass();

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
            }

            if (clazz.isAnnotationPresent(Preprocessor.class)) {
                if (!(instance instanceof RequestInterceptor)) {
                    throw new IllegalStateException(
                            clazz.getName() + " is annotated with @Preprocessor but does not implement RequestInterceptor");
                }
                interceptors.add((RequestInterceptor) instance);
            }
        }

        ExceptionResolver resolver = exceptionHandler != null
                ? new ExceptionResolver(exceptionHandler)
                : ExceptionResolver.none();

        interceptors.sort((a, b) -> {
            int pa = a.getClass().getAnnotation(Preprocessor.class).priority();
            int pb = b.getClass().getAnnotation(Preprocessor.class).priority();
            return Integer.compare(pa, pb);
        });

        logScanResult(routes, interceptors, exceptionHandler);

        return new ScanResult(new RouteRegistry(routes), resolver, List.copyOf(interceptors));
    }

    private static List<Class<?>> discoverClasses(String basePackage) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GeminiController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(GeminiExceptionHandler.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Preprocessor.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                classes.add(Class.forName(bd.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load: " + bd.getBeanClassName(), e);
            }
        }
        return classes;
    }

    /**
     * Instantiates all discovered classes, resolving constructor dependencies.
     * Classes with no-arg constructors are instantiated first. Classes with
     * parameterized constructors are resolved from already-instantiated components.
     * Repeats until all are resolved or no progress is made (circular dependency).
     */
    private static Map<Class<?>, Object> instantiateAll(List<Class<?>> classes) {
        Map<Class<?>, Object> components = new LinkedHashMap<>();
        List<Class<?>> pending = new ArrayList<>(classes);

        while (!pending.isEmpty()) {
            int before = pending.size();
            var iterator = pending.iterator();
            while (iterator.hasNext()) {
                Class<?> clazz = iterator.next();
                Object instance = tryInstantiate(clazz, components);
                if (instance != null) {
                    components.put(clazz, instance);
                    iterator.remove();
                }
            }
            if (pending.size() == before) {
                throw new IllegalStateException(
                        "Cannot resolve dependencies for: " + pending.stream()
                                .map(Class::getSimpleName).toList()
                        + ". Check for circular dependencies or missing components.");
            }
        }

        return components;
    }

    private static Object tryInstantiate(Class<?> clazz, Map<Class<?>, Object> components) {
        Constructor<?> ctor = selectConstructor(clazz);
        Class<?>[] paramTypes = ctor.getParameterTypes();

        if (paramTypes.length == 0) {
            try {
                return ctor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
            }
        }

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Object dep = resolve(paramTypes[i], components);
            if (dep == null) return null;
            args[i] = dep;
        }

        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private static Constructor<?> selectConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length == 1) return ctors[0];
        for (Constructor<?> c : ctors) {
            if (c.getParameterCount() == 0) return c;
        }
        throw new IllegalStateException(
                clazz.getSimpleName() + " has multiple constructors but none are no-arg. "
                + "Use a single constructor for injection.");
    }

    private static Object resolve(Class<?> type, Map<Class<?>, Object> components) {
        for (var entry : components.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void logScanResult(Map<String, HandlerMethod> routes,
                                       List<RequestInterceptor> interceptors,
                                       Object exceptionHandler) {
        routes.forEach((path, handler) ->
                AccessLog.log("Route {} -> {}.{}", path,
                        handler.controller().getClass().getSimpleName(), handler.method().getName()));

        for (var interceptor : interceptors) {
            int priority = interceptor.getClass().getAnnotation(Preprocessor.class).priority();
            AccessLog.log("Interceptor {} priority={}", interceptor.getClass().getSimpleName(), priority);
        }

        if (exceptionHandler != null) {
            AccessLog.log("ExceptionHandler {}", exceptionHandler.getClass().getSimpleName());
        }
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
        String classPathValue = AnnotationSupport.findPath(clazz);
        if (classPathValue != null) {
            classPath = normalize(classPathValue);
        }

        for (Method method : clazz.getDeclaredMethods()) {
            String methodPathValue = AnnotationSupport.findPath(method);
            if (methodPathValue == null) {
                continue;
            }

            String fullPath = classPath + normalize(methodPathValue);
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
