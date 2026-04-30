package io.gemboot.internal;

import java.lang.reflect.Method;

/**
 * An immutable reference to a controller instance and one of its handler methods.
 *
 * @param controller the controller instance that owns the method
 * @param method the reflective handle to the handler method
 */
public record HandlerMethod(Object controller, Method method) {
}
