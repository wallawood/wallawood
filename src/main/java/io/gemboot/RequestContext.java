package io.gemboot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A write-once, type-keyed container for request-scoped data. Built before
 * routing and passed through the interceptor pipeline and into handler
 * invocation.
 *
 * <p>Each type can only be added once. Attempting to add a second instance
 * of the same type throws {@link IllegalStateException}. This prevents
 * downstream code from replacing security-sensitive entries like
 * {@link Grant}.
 *
 * <p>Null values are silently ignored by {@link #add(Object)}.
 */
public final class RequestContext {

    private final Map<Class<?>, Object> context = new LinkedHashMap<>();

    /**
     * Adds an object to the context, keyed by its runtime class.
     * Null values are ignored. Duplicate types are rejected.
     *
     * @param o the object to add
     * @throws IllegalStateException if an entry for this type already exists
     */
    public void add(Object o) {
        if (o == null) return;
        @SuppressWarnings("unchecked")
        Class<Object> key = (Class<Object>) o.getClass();
        add(key, o);
    }

    /**
     * Adds an object to the context under a specific type key. The object
     * must be assignable to {@code key}. Null values are ignored. Duplicate
     * types are rejected.
     *
     * @param key  the type to register under
     * @param value the object to add
     * @throws IllegalArgumentException if {@code value} is not assignable to {@code key}
     * @throws IllegalStateException    if an entry for this type already exists
     */
    public <T> void add(Class<T> key, T value) {
        if (value == null) return;
        if (!key.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(
                    value.getClass().getSimpleName() + " is not assignable to " + key.getSimpleName());
        }
        if (context.containsKey(key)) {
            throw new IllegalStateException(key.getSimpleName() + " already set in RequestContext");
        }
        context.put(key, value);
    }

    /**
     * Retrieves an entry by type, or {@code null} if not present.
     * Tries exact match first, then falls back to assignability.
     *
     * @param clazz the type to look up
     * @return the stored instance, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        Object exact = context.get(clazz);
        if (exact != null) return (T) exact;
        for (var entry : context.entrySet()) {
            if (clazz.isAssignableFrom(entry.getKey())) {
                return (T) entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable view of all entries. Used by {@code @Context}
     * parameter resolution to match by assignability.
     *
     * @return unmodifiable entry set
     */
    public Set<Map.Entry<Class<?>, Object>> entrySet() {
        return Collections.unmodifiableSet(context.entrySet());
    }
}
