package org.server.gemini.internal;

import org.server.gemini.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * Resolves static resources from the classpath under {@code /static/}. This mirrors
 * the convention used by Spring Boot for serving static content.
 *
 * <p>Static resources take precedence over dynamic routes. If a file exists on the
 * classpath at {@code /static/<path>}, it is served directly. Directory requests
 * (paths ending in {@code /}) automatically look for an {@code index.gmi} file.
 *
 * <p>MIME types are inferred from the file extension, defaulting to
 * {@code application/octet-stream} for unknown types. Gemtext files ({@code .gmi})
 * are served as {@code text/gemini; charset=utf-8}.
 */
public final class StaticResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceResolver.class);
    private static final String STATIC_ROOT = "/static";

    private StaticResourceResolver() {
    }

    /**
     * Attempts to resolve the given request path to a static classpath resource.
     *
     * @param path the request URI path (e.g. {@code "/docs/guide.gmi"})
     * @return a {@link GeminiResponse} if a static resource was found, or {@code null}
     */
    public static GeminiResponse resolve(String path) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.endsWith("/")) {
            path = path + "index.gmi";
        }

        String resourcePath = STATIC_ROOT + path;

        if (resourcePath.contains("..")) {
            log.warn("Rejected path traversal attempt: {}", path);
            return GeminiResponse.badRequest("Invalid path");
        }

        try (InputStream in = StaticResourceResolver.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }

            byte[] body = in.readAllBytes();
            String mimeType = guessMimeType(resourcePath);
            log.debug("Serving static resource: {} ({})", resourcePath, mimeType);
            return GeminiResponse.success(mimeType, body);
        } catch (IOException e) {
            log.error("Failed to read static resource: {}", resourcePath, e);
            return GeminiResponse.temporaryFailure("Error reading resource");
        }
    }

    static String guessMimeType(String path) {
        if (path.endsWith(".gmi") || path.endsWith(".gemini")) {
            return "text/gemini; charset=utf-8";
        }
        String guess = URLConnection.guessContentTypeFromName(path);
        return guess != null ? guess : "application/octet-stream";
    }
}
