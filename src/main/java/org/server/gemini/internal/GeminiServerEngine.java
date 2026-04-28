package org.server.gemini.internal;

import io.netty.buffer.ByteBuf;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.server.gemini.GeminiConfig;
import org.server.gemini.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * The Reactor Netty engine that listens for Gemini requests over TLS,
 * runs the request pipeline (static → dynamic → not found), and writes
 * the response. Each connection handles exactly one request-response
 * cycle, then closes with TLS {@code close_notify}.
 */
public final class GeminiServerEngine {

    private static final Logger log = LoggerFactory.getLogger(GeminiServerEngine.class);
    private static final int MAX_REQUEST_BYTES = 1026; // 1024 URI + \r\n
    private static final int READ_TIMEOUT_SECONDS = 5;

    private final RouteRegistry routeRegistry;
    private final CertificateManager certificateManager;
    private final ExceptionResolver exceptionResolver;
    private final GeminiConfig config;

    public GeminiServerEngine(RouteRegistry routeRegistry, CertificateManager certificateManager,
                              ExceptionResolver exceptionResolver, GeminiConfig config) {
        this.routeRegistry = routeRegistry;
        this.certificateManager = certificateManager;
        this.exceptionResolver = exceptionResolver;
        this.config = config;
    }

    /**
     * Starts the server and blocks until it is shut down.
     */
    public void start() {
        DisposableServer server = createServer();
        log.info("Gemini server listening on {}:{}", config.hostname(), config.port());
        server.onDispose().block();
    }

    /**
     * Starts the server and returns the handle for programmatic shutdown.
     */
    public DisposableServer startNonBlocking() {
        DisposableServer server = createServer();
        log.info("Gemini server listening on {}:{}", config.hostname(), config.port());
        return server;
    }

    private DisposableServer createServer() {
        return TcpServer.create()
                .host(config.bindAddress())
                .port(config.port())
                .secure(spec -> spec.sslContext(certificateManager.sslContext()))
                .doOnConnection(conn -> {
                    conn.channel().pipeline().addFirst(
                            new ReadTimeoutHandler(READ_TIMEOUT_SECONDS));
                    handleConnection(conn);
                })
                .bindNow();
    }

    private void handleConnection(Connection conn) {
        conn.inbound().receive()
                .aggregate()
                .asByteArray()
                .flatMap(bytes -> {
                    if (bytes.length > MAX_REQUEST_BYTES) {
                        GeminiResponse response = GeminiResponse.badRequest("URI exceeds 1024 bytes");
                        ByteBuf buf = response.toByteBuf(conn.channel().alloc());
                        return conn.outbound().sendObject(Mono.just(buf)).then();
                    }
                    String raw = new String(bytes, StandardCharsets.UTF_8);
                    GeminiResponse response = handleRequest(raw, conn);
                    ByteBuf buf = response.toByteBuf(conn.channel().alloc());
                    return conn.outbound().sendObject(Mono.just(buf)).then();
                })
                .doFinally(signal -> conn.dispose())
                .subscribe(
                        unused -> {},
                        err -> {
                            log.error("Connection error", err);
                            conn.dispose();
                        }
                );
    }

    private GeminiResponse handleRequest(String raw, Connection conn) {
        if (!raw.endsWith("\r\n")) {
            return GeminiResponse.badRequest("Request must end with CRLF");
        }

        String uriString = raw.substring(0, raw.length() - 2);

        URI uri;
        try {
            uri = URI.create(uriString);
        } catch (IllegalArgumentException e) {
            return GeminiResponse.badRequest("Malformed URI");
        }

        if (uri.getScheme() == null) {
            return GeminiResponse.badRequest("Request must be an absolute URI");
        }

        if (!"gemini".equalsIgnoreCase(uri.getScheme())) {
            return GeminiResponse.proxyRequestRefused("Non-Gemini scheme");
        }

        if (uri.getUserInfo() != null) {
            return GeminiResponse.badRequest("Userinfo not allowed");
        }

        if (uri.getFragment() != null) {
            return GeminiResponse.badRequest("Fragment not allowed");
        }

        String requestHost = uri.getHost();
        if (requestHost != null && !requestHost.equalsIgnoreCase(config.hostname())) {
            return GeminiResponse.proxyRequestRefused();
        }

        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        log.debug("Request: {}", uriString.replaceAll("[\\r\\n]", ""));

        GeminiResponse staticResponse = StaticResourceResolver.resolve(path);
        if (staticResponse != null) {
            return staticResponse;
        }

        RouteRegistry.MatchedRoute matched = routeRegistry.match(path);
        if (matched != null) {
            X509Certificate clientCert = extractClientCert(conn);
            return HandlerInvoker.invoke(matched, uri, clientCert, exceptionResolver);
        }

        return GeminiResponse.notFound();
    }

    private static X509Certificate extractClientCert(Connection conn) {
        SslHandler sslHandler = conn.channel().pipeline().get(SslHandler.class);
        if (sslHandler == null) {
            return null;
        }

        try {
            SSLSession session = sslHandler.engine().getSession();
            Certificate[] certs = session.getPeerCertificates();
            if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate x509) {
                return x509;
            }
        } catch (SSLPeerUnverifiedException e) {
            // No client cert provided — this is normal
        }

        return null;
    }
}
