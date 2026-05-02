package io.gemboot.internal;

import io.gemboot.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs Gemini access events at INFO level on a dedicated logger ({@code io.gemboot.access}).
 * This allows operators to route access logs independently from library internals,
 * which log at DEBUG.
 */
public final class AccessLog {

    private static final Logger log = LoggerFactory.getLogger("io.gemboot.access");

    private AccessLog() {}

    public static void log(String path, GeminiResponse response, long durationMs) {
        if (log.isInfoEnabled()) {
            log.info("{} {} {}ms", path, response.status(), durationMs);
        }
    }

    public static void log(String format, Object... args) {
        if (log.isInfoEnabled()) {
            log.info(format, args);
        }
    }
}
