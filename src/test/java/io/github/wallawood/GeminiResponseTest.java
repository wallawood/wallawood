package io.github.wallawood;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GeminiResponseTest {

    private static final UnpooledByteBufAllocator ALLOC = UnpooledByteBufAllocator.DEFAULT;

    private static String toWireString(GeminiResponse r) {
        ByteBuf buf = r.toByteBuf(ALLOC);
        try {
            return buf.toString(StandardCharsets.UTF_8);
        } finally {
            buf.release();
        }
    }

    private static byte[] toWireBytes(GeminiResponse r) {
        ByteBuf buf = r.toByteBuf(ALLOC);
        try {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    @Test
    void input() {
        var r = GeminiResponse.input("Enter your name:");
        assertEquals(10, r.status());
        assertEquals("Enter your name:", r.meta());
        assertNull(r.body());
        assertEquals("10 Enter your name:\r\n", toWireString(r));
    }

    @Test
    void sensitiveInput() {
        var r = GeminiResponse.sensitiveInput("Password:");
        assertEquals(11, r.status());
        assertEquals("Password:", r.meta());
        assertNull(r.body());
        assertEquals("11 Password:\r\n", toWireString(r));
    }

    @Test
    void successWithMimeTypeAndStringBody() {
        var r = GeminiResponse.success("text/plain", "hello");
        assertEquals(20, r.status());
        assertEquals("text/plain", r.meta());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), r.body());
        assertEquals("20 text/plain\r\nhello", toWireString(r));
    }

    @Test
    void successWithMimeTypeAndByteBody() {
        byte[] body = {0x48, 0x49};
        var r = GeminiResponse.success("application/octet-stream", body);
        assertEquals(20, r.status());
        assertEquals("application/octet-stream", r.meta());
        assertArrayEquals(body, r.body());

        byte[] wire = toWireBytes(r);
        String header = "20 application/octet-stream\r\n";
        assertEquals(header.length() + body.length, wire.length);
    }

    @Test
    void successGemtextStringBody() {
        var r = GeminiResponse.success("# Hello");
        assertEquals(20, r.status());
        assertEquals("text/gemini; charset=utf-8", r.meta());
        assertEquals("20 text/gemini; charset=utf-8\r\n# Hello", toWireString(r));
    }

    @Test
    void successGemtextByteBody() {
        byte[] body = "# Hello".getBytes(StandardCharsets.UTF_8);
        var r = GeminiResponse.success(body);
        assertEquals(20, r.status());
        assertEquals("text/gemini; charset=utf-8", r.meta());
        assertArrayEquals(body, r.body());
    }

    @Test
    void successWithUtf8Body() {
        var r = GeminiResponse.success("Héllo wörld 🌍");
        assertEquals("20 text/gemini; charset=utf-8\r\nHéllo wörld 🌍", toWireString(r));
    }

    @Test
    void successWithBinaryBody() {
        byte[] body = {0x00, 0x01, (byte) 0xFF};
        var r = GeminiResponse.success("application/octet-stream", body);
        byte[] wire = toWireBytes(r);
        byte[] headerBytes = "20 application/octet-stream\r\n".getBytes(StandardCharsets.UTF_8);
        assertEquals(headerBytes.length + body.length, wire.length);
        assertEquals(wire[headerBytes.length], 0x00);
        assertEquals(wire[headerBytes.length + 1], 0x01);
        assertEquals(wire[headerBytes.length + 2], (byte) 0xFF);
    }

    @Test
    void temporaryRedirect() {
        var r = GeminiResponse.temporaryRedirect("/new-path");
        assertEquals(30, r.status());
        assertEquals("/new-path", r.meta());
        assertNull(r.body());
        assertEquals("30 /new-path\r\n", toWireString(r));
    }

    @Test
    void permanentRedirect() {
        var r = GeminiResponse.permanentRedirect("gemini://example.com/moved");
        assertEquals(31, r.status());
        assertEquals("gemini://example.com/moved", r.meta());
        assertEquals("31 gemini://example.com/moved\r\n", toWireString(r));
    }

    @Test
    void temporaryFailureWithMessage() {
        var r = GeminiResponse.temporaryFailure("Try again later");
        assertEquals(40, r.status());
        assertEquals("Try again later", r.meta());
        assertNull(r.body());
        assertEquals("40 Try again later\r\n", toWireString(r));
    }

    @Test
    void temporaryFailureWithoutMessage() {
        var r = GeminiResponse.temporaryFailure();
        assertEquals(40, r.status());
        assertNull(r.meta());
        assertEquals("40 \r\n", toWireString(r));
    }

    @Test
    void serverUnavailableWithMessage() {
        var r = GeminiResponse.serverUnavailable("Undergoing maintenance");
        assertEquals(41, r.status());
        assertEquals("41 Undergoing maintenance\r\n", toWireString(r));
    }

    @Test
    void serverUnavailableWithoutMessage() {
        var r = GeminiResponse.serverUnavailable();
        assertEquals(41, r.status());
        assertEquals("41 \r\n", toWireString(r));
    }

    @Test
    void cgiErrorWithMessage() {
        var r = GeminiResponse.cgiError("Process timed out");
        assertEquals(42, r.status());
        assertEquals("42 Process timed out\r\n", toWireString(r));
    }

    @Test
    void cgiErrorWithoutMessage() {
        var r = GeminiResponse.cgiError();
        assertEquals(42, r.status());
        assertEquals("42 \r\n", toWireString(r));
    }

    @Test
    void proxyErrorWithMessage() {
        var r = GeminiResponse.proxyError("Upstream failed");
        assertEquals(43, r.status());
        assertEquals("43 Upstream failed\r\n", toWireString(r));
    }

    @Test
    void proxyErrorWithoutMessage() {
        var r = GeminiResponse.proxyError();
        assertEquals(43, r.status());
        assertEquals("43 \r\n", toWireString(r));
    }

    @Test
    void slowDownWithMessage() {
        var r = GeminiResponse.slowDown("Rate limited");
        assertEquals(44, r.status());
        assertEquals("44 Rate limited\r\n", toWireString(r));
    }

    @Test
    void slowDownWithoutMessage() {
        var r = GeminiResponse.slowDown();
        assertEquals(44, r.status());
        assertEquals("44 \r\n", toWireString(r));
    }

    @Test
    void permanentFailureWithMessage() {
        var r = GeminiResponse.permanentFailure("Denied");
        assertEquals(50, r.status());
        assertEquals("50 Denied\r\n", toWireString(r));
    }

    @Test
    void permanentFailureWithoutMessage() {
        var r = GeminiResponse.permanentFailure();
        assertEquals(50, r.status());
        assertEquals("50 \r\n", toWireString(r));
    }

    @Test
    void notFoundWithMessage() {
        var r = GeminiResponse.notFound("Resource not found");
        assertEquals(51, r.status());
        assertEquals("51 Resource not found\r\n", toWireString(r));
    }

    @Test
    void notFoundWithoutMessage() {
        var r = GeminiResponse.notFound();
        assertEquals(51, r.status());
        assertEquals("51 \r\n", toWireString(r));
    }

    @Test
    void goneWithMessage() {
        var r = GeminiResponse.gone("Removed permanently");
        assertEquals(52, r.status());
        assertEquals("52 Removed permanently\r\n", toWireString(r));
    }

    @Test
    void goneWithoutMessage() {
        var r = GeminiResponse.gone();
        assertEquals(52, r.status());
        assertEquals("52 \r\n", toWireString(r));
    }

    @Test
    void proxyRequestRefusedWithMessage() {
        var r = GeminiResponse.proxyRequestRefused("Not a proxy");
        assertEquals(53, r.status());
        assertEquals("53 Not a proxy\r\n", toWireString(r));
    }

    @Test
    void proxyRequestRefusedWithoutMessage() {
        var r = GeminiResponse.proxyRequestRefused();
        assertEquals(53, r.status());
        assertEquals("53 \r\n", toWireString(r));
    }

    @Test
    void badRequestWithMessage() {
        var r = GeminiResponse.badRequest("Malformed URI");
        assertEquals(59, r.status());
        assertEquals("59 Malformed URI\r\n", toWireString(r));
    }

    @Test
    void badRequestWithoutMessage() {
        var r = GeminiResponse.badRequest();
        assertEquals(59, r.status());
        assertEquals("59 \r\n", toWireString(r));
    }

    @Test
    void clientCertificateRequiredWithMessage() {
        var r = GeminiResponse.clientCertificateRequired("Please provide a certificate");
        assertEquals(60, r.status());
        assertEquals("60 Please provide a certificate\r\n", toWireString(r));
    }

    @Test
    void clientCertificateRequiredWithoutMessage() {
        var r = GeminiResponse.clientCertificateRequired();
        assertEquals(60, r.status());
        assertEquals("60 \r\n", toWireString(r));
    }

    @Test
    void certificateNotAuthorizedWithMessage() {
        var r = GeminiResponse.certificateNotAuthorized("Access denied");
        assertEquals(61, r.status());
        assertEquals("61 Access denied\r\n", toWireString(r));
    }

    @Test
    void certificateNotAuthorizedWithoutMessage() {
        var r = GeminiResponse.certificateNotAuthorized();
        assertEquals(61, r.status());
        assertEquals("61 \r\n", toWireString(r));
    }

    @Test
    void certificateNotValidWithMessage() {
        var r = GeminiResponse.certificateNotValid("Certificate expired");
        assertEquals(62, r.status());
        assertEquals("62 Certificate expired\r\n", toWireString(r));
    }

    @Test
    void certificateNotValidWithoutMessage() {
        var r = GeminiResponse.certificateNotValid();
        assertEquals(62, r.status());
        assertEquals("62 \r\n", toWireString(r));
    }

    @Test
    void toStringWithBodyIncludesSize() {
        var r = GeminiResponse.success("text/plain", "hello");
        assertEquals("<20 Success,text/plain,5 bytes>", r.toString());
    }

    @Test
    void toStringWithoutBody() {
        var r = GeminiResponse.notFound("Resource not found");
        assertEquals("<51 Not found,Resource not found>", r.toString());
    }

    @Test
    void toStringWithoutMeta() {
        var r = GeminiResponse.permanentFailure();
        assertEquals("<50 Permanent failure>", r.toString());
    }

    @Test
    void toByteBufIsCorrectlySized() {
        var r = GeminiResponse.success("text/plain", "hello");
        var buf = r.toByteBuf(ALLOC);
        try {
            assertEquals("20 text/plain\r\nhello".getBytes(StandardCharsets.UTF_8).length, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void toByteBufCanBeCalledMultipleTimes() {
        var r = GeminiResponse.notFound("gone");
        for (int i = 0; i < 3; i++) {
            var buf = r.toByteBuf(ALLOC);
            try {
                assertEquals("51 gone\r\n", buf.toString(StandardCharsets.UTF_8));
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void customWithBody() {
        var r = GeminiResponse.custom(21, "text/plain", "hi".getBytes(StandardCharsets.UTF_8));
        assertEquals(21, r.status());
        assertEquals("text/plain", r.meta());
        assertEquals("21 text/plain\r\nhi", toWireString(r));
    }

    @Test
    void customWithoutBody() {
        var r = GeminiResponse.custom(45, "Rate limited", null);
        assertEquals(45, r.status());
        assertNull(r.body());
        assertEquals("45 Rate limited\r\n", toWireString(r));
    }

    @Test
    void customToStringOmitsReason() {
        var r = GeminiResponse.custom(99, "custom meta", null);
        assertEquals("<99,custom meta>", r.toString());
    }
}
