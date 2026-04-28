package org.server.gemini;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a handler method or all methods in a controller require the client
 * to present a TLS client certificate. If the client does not provide a certificate,
 * the server automatically responds with status 60 (Client Certificate Required)
 * without invoking the handler.
 *
 * <p>Per the Gemini protocol specification, the scope of a client certificate should
 * be limited to the host, port, and path of the original request. Clients must not
 * reuse a certificate across different hosts or ports without user consent.
 *
 * <p>Can be applied at the method level to protect individual routes, or at the
 * class level to protect all routes in a controller.
 *
 * <pre>{@code
 * @GeminiController
 * @Path("/private")
 * @RequireCertificate
 * public class PrivateController {
 *
 *     @Path("/profile")
 *     public GeminiResponse profile(@Context X509Certificate cert) {
 *         return GeminiResponse.success("# Hello, " + cert.getSubjectX500Principal());
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireCertificate {
    /**
     * Optional message sent to the client with the status 60 response.
     * Per the Gemini spec, servers SHOULD include information about why
     * a certificate is required.
     *
     * @return the message, or empty string for no message
     */
    String value() default "";
}
