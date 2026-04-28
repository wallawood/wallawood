package org.server.gemini.internal;

import java.lang.reflect.Method;

/**
 * An immutable reference to a controller instance and one of its handler methods.
 */
public final class HandlerMethod {

    private final Object controller;
    private final Method method;

    public HandlerMethod(Object controller, Method method) {
        this.controller = controller;
        this.method = method;
    }

    public Object controller() {
        return controller;
    }

    public Method method() {
        return method;
    }
}
