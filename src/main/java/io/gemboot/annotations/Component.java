package io.gemboot.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a component to be discovered during classpath scanning
 * and made available for constructor injection into controllers, preprocessors,
 * and other components.
 *
 * <p>Components are singletons — one instance is created and shared across
 * all dependents.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
