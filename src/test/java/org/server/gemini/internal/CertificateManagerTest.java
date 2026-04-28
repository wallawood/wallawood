package org.server.gemini.internal;

import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CertificateManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesNewCertificate() {
        CertificateManager cm = CertificateManager.loadOrGenerate(tempDir.resolve("certs"));

        assertTrue(Files.exists(tempDir.resolve("certs/cert.pem")));
        assertTrue(Files.exists(tempDir.resolve("certs/key.pem")));
        assertNotNull(cm);
    }

    @Test
    void reusesExistingCertificate() {
        Path certDir = tempDir.resolve("certs");
        CertificateManager ignored = CertificateManager.loadOrGenerate(certDir);
        long certModified = certDir.resolve("cert.pem").toFile().lastModified();

        CertificateManager second = CertificateManager.loadOrGenerate(certDir);
        long certModifiedAfter = certDir.resolve("cert.pem").toFile().lastModified();

        assertEquals(certModified, certModifiedAfter);
        assertNotNull(second);
    }

    @Test
    void loadFromGeneratedFiles() {
        Path certDir = tempDir.resolve("certs");
        CertificateManager.loadOrGenerate(certDir);

        CertificateManager loaded = CertificateManager.load(
                certDir.resolve("cert.pem"),
                certDir.resolve("key.pem")
        );
        assertNotNull(loaded);
    }

    @Test
    void loadFailsWithMissingCert() {
        assertThrows(IllegalArgumentException.class,
                () -> CertificateManager.load(tempDir.resolve("nope.pem"), tempDir.resolve("nope.pem")));
    }

    @Test
    void buildsSslContext() {
        CertificateManager cm = CertificateManager.loadOrGenerate(tempDir.resolve("certs"));
        SslContext ctx = cm.sslContext();
        assertNotNull(ctx);
        assertTrue(ctx.isServer());
    }

    @Test
    void generatedCertIsPemFormat() throws Exception {
        CertificateManager.loadOrGenerate(tempDir.resolve("certs"));
        String certContent = Files.readString(tempDir.resolve("certs/cert.pem"));
        String keyContent = Files.readString(tempDir.resolve("certs/key.pem"));

        assertTrue(certContent.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(keyContent.startsWith("-----BEGIN EC PRIVATE KEY-----"));
    }
}
