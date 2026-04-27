package org.server.gemini;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;

/**
 * An immutable representation of a Gemini protocol response.
 *
 * <p>A Gemini response consists of a two-digit status code, an optional meta string
 * (whose meaning varies by status category), and an optional body (only for success responses).
 * The wire format is: {@code <STATUS><SP><META><CR><LF>[body]}.
 *
 * <p>Instances are created via static factory methods corresponding to each defined status code.
 * Use {@link #toByteBuf(ByteBufAllocator)} to serialize the response for transmission over
 * a Netty channel.
 *
 * @see <a href="https://geminiprotocol.net/docs/protocol-specification.gmi">Gemini Protocol Specification</a>
 */
public final class GeminiResponse {

    private final StatusCodes status;
    private final String meta;
    private final byte[] body;

    private GeminiResponse(StatusCodes status, String meta, byte[] body) {
        this.status = status;
        this.meta = meta;
        this.body = body;
    }

    /**
     * Creates a status 10 (INPUT) response. The server is requesting input from the client.
     * The prompt will be displayed to the user, and the client should re-request the same URI
     * with the user's input URI-encoded as the query component.
     *
     * @param prompt the text to display to the user when requesting input
     * @return a Gemini input response
     */
    public static GeminiResponse input(String prompt) {
        return new GeminiResponse(StatusCodes.INPUT, prompt, null);
    }

    /**
     * Creates a status 11 (SENSITIVE INPUT) response. Behaves identically to {@link #input(String)},
     * but the client should not echo the user's input to the screen, as it may contain
     * sensitive information such as a password.
     *
     * @param prompt the text to display to the user when requesting sensitive input
     * @return a Gemini sensitive input response
     */
    public static GeminiResponse sensitiveInput(String prompt) {
        return new GeminiResponse(StatusCodes.SENSITIVE_INPUT, prompt, null);
    }

    /**
     * Creates a status 20 (SUCCESS) response with the given MIME type and binary body.
     * The server has successfully handled the request and will serve content of the
     * specified MIME type. The connection is closed after the final byte of the body.
     *
     * @param mimeType the MIME type of the content (e.g. {@code "text/gemini; charset=utf-8"})
     * @param body the raw response body bytes
     * @return a Gemini success response
     */
    public static GeminiResponse success(String mimeType, byte[] body) {
        return new GeminiResponse(StatusCodes.SUCCESS, mimeType, body);
    }

    /**
     * Creates a status 20 (SUCCESS) response with the given MIME type and UTF-8 encoded string body.
     *
     * @param mimeType the MIME type of the content (e.g. {@code "text/plain"})
     * @param body the response body as a string, encoded as UTF-8
     * @return a Gemini success response
     * @see #success(String, byte[])
     */
    public static GeminiResponse success(String mimeType, String body) {
        return success(mimeType, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a status 20 (SUCCESS) response with a MIME type of
     * {@code text/gemini; charset=utf-8} and the given binary body. This is a convenience
     * method for serving Gemini-native content (gemtext).
     *
     * @param body the raw gemtext body bytes
     * @return a Gemini success response with gemtext MIME type
     * @see #success(String, byte[])
     */
    public static GeminiResponse success(byte[] body) {
        return success("text/gemini; charset=utf-8", body);
    }

    /**
     * Creates a status 20 (SUCCESS) response with a MIME type of
     * {@code text/gemini; charset=utf-8} and the given string body encoded as UTF-8.
     * This is a convenience method for serving Gemini-native content (gemtext).
     *
     * @param body the gemtext body as a string
     * @return a Gemini success response with gemtext MIME type
     * @see #success(String, String)
     */
    public static GeminiResponse success(String body) {
        return success("text/gemini; charset=utf-8", body);
    }

    /**
     * Creates a status 30 (TEMPORARY REDIRECT) response. The requested resource should be
     * retrieved from the given URI instead, but the client should continue to use the
     * original URI for future requests.
     *
     * @param uri the absolute or relative URI to redirect to
     * @return a Gemini temporary redirect response
     */
    public static GeminiResponse temporaryRedirect(String uri) {
        return new GeminiResponse(StatusCodes.TEMPORARY_REDIRECT, uri, null);
    }

    /**
     * Creates a status 31 (PERMANENT REDIRECT) response. The requested resource has permanently
     * moved to the given URI. Clients should use the new URI for all future requests to
     * this resource.
     *
     * @param uri the absolute or relative URI of the new permanent location
     * @return a Gemini permanent redirect response
     */
    public static GeminiResponse permanentRedirect(String uri) {
        return new GeminiResponse(StatusCodes.PERMANENT_REDIRECT, uri, null);
    }

    /**
     * Creates a status 40 (TEMPORARY FAILURE) response. An unspecified condition on the server
     * is preventing the content from being served. The client may retry the request.
     *
     * @param message an optional human-readable description of the failure
     * @return a Gemini temporary failure response
     */
    public static GeminiResponse temporaryFailure(String message) {
        return new GeminiResponse(StatusCodes.TEMPORARY_FAILURE, message, null);
    }

    /**
     * Creates a status 40 (TEMPORARY FAILURE) response with no error message.
     *
     * @return a Gemini temporary failure response
     * @see #temporaryFailure(String)
     */
    public static GeminiResponse temporaryFailure() {
        return new GeminiResponse(StatusCodes.TEMPORARY_FAILURE, null, null);
    }

    /**
     * Creates a status 41 (SERVER UNAVAILABLE) response. The server is unavailable due to
     * overload or maintenance. Analogous to HTTP 503.
     *
     * @param message an optional human-readable description of the outage
     * @return a Gemini server unavailable response
     */
    public static GeminiResponse serverUnavailable(String message) {
        return new GeminiResponse(StatusCodes.SERVER_UNAVAILABLE, message, null);
    }

    /**
     * Creates a status 41 (SERVER UNAVAILABLE) response with no error message.
     *
     * @return a Gemini server unavailable response
     * @see #serverUnavailable(String)
     */
    public static GeminiResponse serverUnavailable() {
        return new GeminiResponse(StatusCodes.SERVER_UNAVAILABLE, null, null);
    }

    /**
     * Creates a status 42 (CGI ERROR) response. A CGI process or similar dynamic content
     * system died unexpectedly or timed out.
     *
     * @param message an optional human-readable description of the error
     * @return a Gemini CGI error response
     */
    public static GeminiResponse cgiError(String message) {
        return new GeminiResponse(StatusCodes.CGI_ERROR, message, null);
    }

    /**
     * Creates a status 42 (CGI ERROR) response with no error message.
     *
     * @return a Gemini CGI error response
     * @see #cgiError(String)
     */
    public static GeminiResponse cgiError() {
        return new GeminiResponse(StatusCodes.CGI_ERROR, null, null);
    }

    /**
     * Creates a status 43 (PROXY ERROR) response. A proxy request failed because the server
     * was unable to successfully complete a transaction with the remote host.
     * Analogous to HTTP 502/504.
     *
     * @param message an optional human-readable description of the proxy failure
     * @return a Gemini proxy error response
     */
    public static GeminiResponse proxyError(String message) {
        return new GeminiResponse(StatusCodes.PROXY_ERROR, message, null);
    }

    /**
     * Creates a status 43 (PROXY ERROR) response with no error message.
     *
     * @return a Gemini proxy error response
     * @see #proxyError(String)
     */
    public static GeminiResponse proxyError() {
        return new GeminiResponse(StatusCodes.PROXY_ERROR, null, null);
    }

    /**
     * Creates a status 44 (SLOW DOWN) response. The server is requesting the client to
     * slow down its requests. The client should use exponential backoff, doubling the
     * delay between subsequent requests until this status is no longer returned.
     *
     * @param message an optional human-readable description
     * @return a Gemini slow down response
     */
    public static GeminiResponse slowDown(String message) {
        return new GeminiResponse(StatusCodes.SLOW_DOWN, message, null);
    }

    /**
     * Creates a status 44 (SLOW DOWN) response with no error message.
     *
     * @return a Gemini slow down response
     * @see #slowDown(String)
     */
    public static GeminiResponse slowDown() {
        return new GeminiResponse(StatusCodes.SLOW_DOWN, null, null);
    }

    /**
     * Creates a status 50 (PERMANENT FAILURE) response. The request has failed permanently.
     * Identical future requests will reliably fail and should not be retried.
     *
     * @param message an optional human-readable description of the failure
     * @return a Gemini permanent failure response
     */
    public static GeminiResponse permanentFailure(String message) {
        return new GeminiResponse(StatusCodes.PERMANENT_FAILURE, message, null);
    }

    /**
     * Creates a status 50 (PERMANENT FAILURE) response with no error message.
     *
     * @return a Gemini permanent failure response
     * @see #permanentFailure(String)
     */
    public static GeminiResponse permanentFailure() {
        return new GeminiResponse(StatusCodes.PERMANENT_FAILURE, null, null);
    }

    /**
     * Creates a status 51 (NOT FOUND) response. The requested resource could not be found
     * and no further information is available. It may or may not exist in the future.
     *
     * @param message an optional human-readable description
     * @return a Gemini not found response
     */
    public static GeminiResponse notFound(String message) {
        return new GeminiResponse(StatusCodes.NOT_FOUND, message, null);
    }

    /**
     * Creates a status 51 (NOT FOUND) response with no error message.
     *
     * @return a Gemini not found response
     * @see #notFound(String)
     */
    public static GeminiResponse notFound() {
        return new GeminiResponse(StatusCodes.NOT_FOUND, null, null);
    }

    /**
     * Creates a status 52 (GONE) response. The requested resource is no longer available
     * and will not be available again. Search engines and content aggregators should remove
     * this resource from their indices. Analogous to HTTP 410.
     *
     * @param message an optional human-readable description
     * @return a Gemini gone response
     */
    public static GeminiResponse gone(String message) {
        return new GeminiResponse(StatusCodes.GONE, message, null);
    }

    /**
     * Creates a status 52 (GONE) response with no error message.
     *
     * @return a Gemini gone response
     * @see #gone(String)
     */
    public static GeminiResponse gone() {
        return new GeminiResponse(StatusCodes.GONE, null, null);
    }

    /**
     * Creates a status 53 (PROXY REQUEST REFUSED) response. The request was for a resource
     * at a domain not served by this server, and the server does not accept proxy requests.
     *
     * @param message an optional human-readable description
     * @return a Gemini proxy request refused response
     */
    public static GeminiResponse proxyRequestRefused(String message) {
        return new GeminiResponse(StatusCodes.PROXY_REQUEST_REFUSED, message, null);
    }

    /**
     * Creates a status 53 (PROXY REQUEST REFUSED) response with no error message.
     *
     * @return a Gemini proxy request refused response
     * @see #proxyRequestRefused(String)
     */
    public static GeminiResponse proxyRequestRefused() {
        return new GeminiResponse(StatusCodes.PROXY_REQUEST_REFUSED, null, null);
    }

    /**
     * Creates a status 59 (BAD REQUEST) response. The server was unable to parse the client's
     * request, presumably due to a malformed request or a violation of the constraints
     * defined in the Gemini specification (e.g. URI exceeding 1024 bytes, presence of
     * an userinfo component, or inclusion of a fragment).
     *
     * @param message an optional human-readable description of the parsing failure
     * @return a Gemini bad request response
     */
    public static GeminiResponse badRequest(String message) {
        return new GeminiResponse(StatusCodes.BAD_REQUEST, message, null);
    }

    /**
     * Creates a status 59 (BAD REQUEST) response with no error message.
     *
     * @return a Gemini bad request response
     * @see #badRequest(String)
     */
    public static GeminiResponse badRequest() {
        return new GeminiResponse(StatusCodes.BAD_REQUEST, null, null);
    }

    /**
     * Creates a status 60 (CLIENT CERTIFICATE REQUIRED) response. The requested resource
     * requires a client certificate for access. The client must provide a certificate and
     * should not repeat the request without one. The certificate scope should be limited to
     * the host, port, and path of the original request (including all paths below it).
     *
     * @param message an optional human-readable description of why a certificate is required
     * @return a Gemini client certificate required response
     */
    public static GeminiResponse clientCertificateRequired(String message) {
        return new GeminiResponse(StatusCodes.CLIENT_CERTIFICATE_REQUIRED, message, null);
    }

    /**
     * Creates a status 60 (CLIENT CERTIFICATE REQUIRED) response with no error message.
     *
     * @return a Gemini client certificate required response
     * @see #clientCertificateRequired(String)
     */
    public static GeminiResponse clientCertificateRequired() {
        return new GeminiResponse(StatusCodes.CLIENT_CERTIFICATE_REQUIRED, null, null);
    }

    /**
     * Creates a status 61 (CERTIFICATE NOT AUTHORIZED) response. The supplied client
     * certificate is not authorized for the requested resource. The certificate itself
     * may be valid and authorized for other resources.
     *
     * @param message an optional human-readable description of the authorization failure
     * @return a Gemini certificate not authorized response
     */
    public static GeminiResponse certificateNotAuthorized(String message) {
        return new GeminiResponse(StatusCodes.CERTIFICATE_NOT_AUTHORIZED, message, null);
    }

    /**
     * Creates a status 61 (CERTIFICATE NOT AUTHORIZED) response with no error message.
     *
     * @return a Gemini certificate not authorized response
     * @see #certificateNotAuthorized(String)
     */
    public static GeminiResponse certificateNotAuthorized() {
        return new GeminiResponse(StatusCodes.CERTIFICATE_NOT_AUTHORIZED, null, null);
    }

    /**
     * Creates a status 62 (CERTIFICATE NOT VALID) response. The supplied client certificate
     * is not valid. This indicates a problem with the certificate itself — most commonly
     * that its validity start date is in the future, its expiry date has passed, it has
     * an invalid signature, or it violates X.509 requirements.
     *
     * @param message an optional human-readable description of the certificate problem
     * @return a Gemini certificate not valid response
     */
    public static GeminiResponse certificateNotValid(String message) {
        return new GeminiResponse(StatusCodes.CERTIFICATE_NOT_VALID, message, null);
    }

    /**
     * Creates a status 62 (CERTIFICATE NOT VALID) response with no error message.
     *
     * @return a Gemini certificate not valid response
     * @see #certificateNotValid(String)
     */
    public static GeminiResponse certificateNotValid() {
        return new GeminiResponse(StatusCodes.CERTIFICATE_NOT_VALID, null, null);
    }

    /**
     * Returns the two-digit Gemini status code.
     *
     * @return the status code (10–62)
     */
    public int status() {
        return status.code();
    }

    /**
     * Returns the meta string. Its meaning depends on the status category:
     * <ul>
     *   <li>1x (Input): the prompt text displayed to the user</li>
     *   <li>2x (Success): the MIME type of the response body</li>
     *   <li>3x (Redirect): the absolute or relative URI to redirect to</li>
     *   <li>4x/5x/6x (Failure/Auth): an optional human-readable error message</li>
     * </ul>
     *
     * @return the meta string, or {@code null} if not provided
     */
    public String meta() {
        return meta;
    }

    /**
     * Returns the raw response body bytes. Only present for success (2x) responses.
     *
     * @return the body bytes, or {@code null} for non-success responses
     */
    public byte[] body() {
        return body;
    }

    /**
     * Returns a debug-friendly string representation of this response, following the
     * convention used by Spring's {@code HttpEntity}:
     * {@code <status reason,meta,body size>}.
     *
     * @return a string representation of this response
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(status.code()).append(' ').append(status.reason());
        if (meta != null) {
            sb.append(',').append(meta);
        }
        if (body != null) {
            sb.append(',').append(body.length).append(" bytes");
        }
        sb.append('>');
        return sb.toString();
    }

    /**
     * Serializes this response into a Netty {@link ByteBuf} for transmission over a channel.
     * The buffer is allocated from the provided allocator and sized exactly to fit the
     * complete wire-format response: {@code <STATUS><SP><META><CR><LF>[body]}.
     *
     * <p>The caller is responsible for releasing the returned buffer.
     *
     * @param alloc the {@link ByteBufAllocator} to use, typically obtained from
     *              {@code channel.alloc()}
     * @return a {@link ByteBuf} containing the complete Gemini response
     */
    public ByteBuf toByteBuf(ByteBufAllocator alloc) {
        String header = meta != null
                ? status.code() + " " + meta + "\r\n"
                : status.code() + "\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        int bodyLen = body != null ? body.length : 0;

        ByteBuf buf = alloc.buffer(headerBytes.length + bodyLen);
        buf.writeBytes(headerBytes);
        if (body != null) {
            buf.writeBytes(body);
        }
        return buf;
    }
}
