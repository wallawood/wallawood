package io.github.wallawood.internal;

import io.github.wallawood.annotations.Path;
import org.junit.jupiter.api.Test;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.internal.fixtures.RootController;
import io.github.wallawood.internal.fixtures.UserController;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteScannerTest {

    @GeminiController
    static class BadReturnController {
        @Path("/bad")
        public String bad() {
            return "oops";
        }
    }

    @Test
    void registersControllerWithClassLevelPath() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        RouteScanner.registerController(new UserController(), routes);

        assertTrue(routes.containsKey("/users"));
        assertTrue(routes.containsKey("/users/{id}"));
        assertTrue(routes.containsKey("/users/search"));
        assertEquals(3, routes.size());
    }

    @Test
    void registersControllerWithoutClassLevelPath() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        RouteScanner.registerController(new RootController(), routes);

        assertTrue(routes.containsKey("/"));
        assertTrue(routes.containsKey("/about"));
        assertEquals(2, routes.size());
    }

    @Test
    void rejectsClassWithoutAnnotation() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> RouteScanner.registerController(new Object(), routes));
    }

    @Test
    void scanFindsControllersInPackage() {
        ScanResult result = RouteScanner.scan("io.github.wallawood.internal.fixtures");
        RouteRegistry registry = result.routeRegistry();

        assertNotNull(registry.match("/users"));
        assertNotNull(registry.match("/users/42"));
        assertNotNull(registry.match("/users/search"));
        assertNotNull(registry.match("/"));
        assertNotNull(registry.match("/about"));
        assertEquals(5, registry.routes().size());
    }

    @Test
    void matchExtractsPathVariables() {
        ScanResult result = RouteScanner.scan("io.github.wallawood.internal.fixtures");
        RouteRegistry registry = result.routeRegistry();

        var match = registry.match("/users/42");
        assertNotNull(match);
        assertEquals("42", match.pathVariables().get("id"));
    }

    @Test
    void noMatchReturnsNull() {
        ScanResult result = RouteScanner.scan("io.github.wallawood.internal.fixtures");
        RouteRegistry registry = result.routeRegistry();
        assertNull(registry.match("/nonexistent"));
    }

    @Test
    void rejectsHandlerWithWrongReturnType() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        assertThrows(IllegalStateException.class,
                () -> RouteScanner.registerController(new BadReturnController(), routes));
    }

    @Test
    void detectsDuplicateRoutes() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        RouteScanner.registerController(new RootController(), routes);
        assertThrows(IllegalStateException.class,
                () -> RouteScanner.registerController(new RootController(), routes));
    }

    @Test
    void constructorInjectionResolvesComponents() {
        ScanResult result = RouteScanner.scan("io.github.wallawood.internal.difixtures");
        assertNotNull(result.routeRegistry().match("/greet"));
    }

    @Test
    void circularDependencyFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> RouteScanner.scan("io.github.wallawood.internal.circularfixtures"));
    }

    @Test
    void literalRoutesMatchBeforeParameterized() {
        Map<String, HandlerMethod> routes = new LinkedHashMap<>();
        // Register parameterized first to prove sorting fixes order
        var controller = new RootController();
        routes.put("/items/{id}", new HandlerMethod(controller, controller.getClass().getDeclaredMethods()[0]));
        routes.put("/items/best", new HandlerMethod(controller, controller.getClass().getDeclaredMethods()[0]));

        var registry = new RouteRegistry(routes);
        var match = registry.match("/items/best");
        assertNotNull(match);
        assertTrue(match.pathVariables().isEmpty());
    }
}
