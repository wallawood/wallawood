package org.server.gemini;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a global exception handler for the Gemini server. The class
 * is discovered during classpath scanning alongside {@link GeminiController @GeminiController}
 * classes.
 *
 * <p>Methods in this class should accept a single exception parameter and return
 * {@link GeminiResponse}. When a handler method throws an exception, the most
 * specific matching method is invoked. If no match is found, the default behavior
 * returns status 40 (Temporary Failure).
 *
 * <p>Only one class may be annotated with {@code @GeminiExceptionHandler} per
 * application. The server will fail fast at startup if multiple are found.
 *
 * <pre>{@code
 * @GeminiExceptionHandler
 * public class ErrorHandler {
 *
 *     public GeminiResponse handle(NotFoundException e) {
 *         return GeminiResponse.notFound(e.getMessage());
 *     }
 *
 *     public GeminiResponse handle(Exception e) {
 *         return GeminiResponse.temporaryFailure("Something went wrong");
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GeminiExceptionHandler {
}
