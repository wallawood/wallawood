package io.github.wallawood.internal;

import io.github.wallawood.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves static resources from an ordered list of directories. The built-in
 * {@code classpath:/static} is always checked first, followed by any user-configured
 * directories in order. Each directory uses a {@code classpath:} or {@code file:} prefix.
 *
 * <p>Static resources take precedence over dynamic routes. The first directory that
 * contains a matching resource wins. Directory requests (paths ending in {@code /})
 * automatically look for an {@code index.gmi} file.
 *
 * <p>MIME types are inferred from the file extension, defaulting to
 * {@code application/octet-stream} for unknown types. Gemtext files ({@code .gmi})
 * are served as {@code text/gemini; charset=utf-8}.
 */
public final class StaticResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceResolver.class);
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private StaticResourceResolver() {
    }

    /**
     * Attempts to resolve the given request path to a static resource by traversing
     * the built-in {@code classpath:/static} first, then each user-configured directory
     * in order. First match wins.
     *
     * @param path the request URI path (e.g. {@code "/docs/guide.gmi"})
     * @param staticDirectories additional directories to search after {@code classpath:/static}
     * @return a {@link GeminiResponse} if a static resource was found, or {@code null}
     */
    public static GeminiResponse resolve(String path, List<String> staticDirectories) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.contains("..") || path.contains("\0")) {
            log.warn("Rejected path traversal attempt: {}", path.replaceAll("[\\r\\n]", ""));
            return GeminiResponse.badRequest("Invalid path");
        }

        String resolvedPath = path.endsWith("/") ? path + "index.gmi" : path;

        GeminiResponse response = resolveFromClasspath("/static", resolvedPath);
        if (response != null) return response;

        for (String dir : staticDirectories) {
            if (dir.startsWith(CLASSPATH_PREFIX)) {
                response = resolveFromClasspath(dir.substring(CLASSPATH_PREFIX.length()), resolvedPath);
            } else if (dir.startsWith(FILE_PREFIX)) {
                response = resolveFromFilesystem(Path.of(dir.substring(FILE_PREFIX.length())), resolvedPath);
            }
            if (response != null) return response;
        }

        return null;
    }

    /**
     * Resolves from the built-in {@code classpath:/static} only (backwards-compatible).
     *
     * @param path the request URI path
     * @return a {@link GeminiResponse} if found, or {@code null}
     */
    public static GeminiResponse resolve(String path) {
        return resolve(path, List.of());
    }

    private static GeminiResponse resolveFromClasspath(String root, String resolvedPath) {
        String resourcePath = root + resolvedPath;

        String normalized = Path.of(resourcePath).normalize().toString();
        if (!normalized.startsWith(root)) {
            return null;
        }

        try {
            if (!resourcePath.contains(".") || resourcePath.lastIndexOf('/') > resourcePath.lastIndexOf('.')) {
                GeminiResponse indexResponse = tryClasspathIndex(resourcePath);
                if (indexResponse != null) return indexResponse;
            }

            try (InputStream in = StaticResourceResolver.class.getResourceAsStream(resourcePath)) {
                if (in == null) return null;
                byte[] body = in.readAllBytes();
                String mimeType = guessMimeType(resourcePath);
                log.debug("Serving static resource: {} ({})", resourcePath, mimeType);
                return GeminiResponse.success(mimeType, body);
            }
        } catch (IOException e) {
            log.error("Failed to read static resource: {}", resourcePath, e);
            return GeminiResponse.temporaryFailure("Error reading resource");
        }
    }

    private static GeminiResponse resolveFromFilesystem(Path root, String resolvedPath) {
        Path filePath = root.resolve(resolvedPath.substring(1)).normalize();
        if (!filePath.startsWith(root.normalize())) {
            return null;
        }

        try {
            if (Files.isRegularFile(filePath)) {
                return serveFile(filePath);
            }

            if (!resolvedPath.contains(".") || resolvedPath.lastIndexOf('/') > resolvedPath.lastIndexOf('.')) {
                Path indexPath = filePath.resolve("index.gmi");
                if (Files.isRegularFile(indexPath)) return serveFile(indexPath);

                Path gmiPath = filePath.getParent().resolve(filePath.getFileName() + ".gmi");
                if (Files.isRegularFile(gmiPath)) return serveFile(gmiPath);
            }
        } catch (IOException e) {
            log.error("Failed to read file resource: {}", filePath, e);
            return GeminiResponse.temporaryFailure("Error reading resource");
        }

        return null;
    }

    private static GeminiResponse serveFile(Path filePath) throws IOException {
        byte[] body = Files.readAllBytes(filePath);
        String mimeType = guessMimeType(filePath.toString());
        log.debug("Serving file resource: {} ({})", filePath, mimeType);
        return GeminiResponse.success(mimeType, body);
    }

    private static GeminiResponse tryClasspathIndex(String resourcePath) {
        String indexPath = resourcePath + "/index.gmi";
        try (InputStream idx = StaticResourceResolver.class.getResourceAsStream(indexPath)) {
            if (idx != null) {
                byte[] body = idx.readAllBytes();
                String mimeType = guessMimeType(indexPath);
                log.debug("Serving static resource: {} ({})", indexPath, mimeType);
                return GeminiResponse.success(mimeType, body);
            }
        } catch (IOException e) {
            // fall through
        }

        String gmiPath = resourcePath + ".gmi";
        try (InputStream gmi = StaticResourceResolver.class.getResourceAsStream(gmiPath)) {
            if (gmi != null) {
                byte[] body = gmi.readAllBytes();
                String mimeType = guessMimeType(gmiPath);
                log.debug("Serving static resource: {} ({})", gmiPath, mimeType);
                return GeminiResponse.success(mimeType, body);
            }
        } catch (IOException e) {
            // fall through
        }

        return null;
    }

    static String guessMimeType(String path) {
        if (path.endsWith(".gmi") || path.endsWith(".gemini")) {
            return "text/gemini; charset=utf-8";
        }
        String guess = URLConnection.guessContentTypeFromName(path);
        return guess != null ? guess : "application/octet-stream";
    }
}
