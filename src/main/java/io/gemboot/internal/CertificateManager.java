package io.gemboot.internal;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

/**
 * Manages TLS certificates for the Gemini server. Supports loading user-provided
 * PEM files or auto-generating a self-signed certificate for development.
 *
 * <p>Auto-generated certificates use EC (prime256v1) keys and are valid for 10 years,
 * matching the common Gemini convention. Generated files are persisted to disk and
 * reused on subsequent starts to maintain TOFU (Trust on First Use) compatibility.
 *
 * <p>The resulting {@link SslContext} is configured for TLS 1.2+ with optional
 * client certificate support, per the Gemini protocol specification.
 */
public final class CertificateManager {

    private static final Logger log = LoggerFactory.getLogger(CertificateManager.class);
    private static final String CERT_FILE = "cert.pem";
    private static final String KEY_FILE = "key.pem";

    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    private CertificateManager(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    /**
     * Loads a certificate and private key from user-provided PEM files.
     *
     * @param certPath path to the certificate PEM file
     * @param keyPath path to the private key PEM file
     * @return a configured {@link CertificateManager}
     * @throws IllegalArgumentException if either file does not exist
     */
    public static CertificateManager load(Path certPath, Path keyPath) {
        if (!Files.exists(certPath)) {
            throw new IllegalArgumentException("Certificate file not found: " + certPath);
        }
        if (!Files.exists(keyPath)) {
            throw new IllegalArgumentException("Key file not found: " + keyPath);
        }

        try {
            X509Certificate cert = readCertificate(certPath);
            PrivateKey key = readPrivateKey(keyPath);
            log.debug("Loaded certificate from {}", certPath);
            return new CertificateManager(cert, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load certificate", e);
        }
    }

    /**
     * Loads existing PEM files from the given directory, or generates a new
     * self-signed certificate if none exist. Generated certificates use EC
     * (prime256v1) keys and are valid for 10 years.
     *
     * @param directory the directory to store/load PEM files
     * @param hostname the hostname for the certificate CN
     * @return a configured {@link CertificateManager}
     */
    public static CertificateManager loadOrGenerate(Path directory, String hostname) {
        Path certPath = directory.resolve(CERT_FILE);
        Path keyPath = directory.resolve(KEY_FILE);

        if (Files.exists(certPath) && Files.exists(keyPath)) {
            log.debug("Found existing certificate in {}", directory);
            return load(certPath, keyPath);
        }

        try {
            Files.createDirectories(directory);

            Security.addProvider(new BouncyCastleProvider());

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            keyGen.initialize(new ECGenParameterSpec("prime256v1"));
            KeyPair keyPair = keyGen.generateKeyPair();

            Instant now = Instant.now();
            X500Name subject = new X500Name("CN=" + hostname);
            BigInteger serial = BigInteger.valueOf(now.toEpochMilli());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setProvider("BC")
                    .build(keyPair.getPrivate());

            X509CertificateHolder holder = new JcaX509v3CertificateBuilder(
                    subject,
                    serial,
                    Date.from(now),
                    Date.from(now.plus(3650, ChronoUnit.DAYS)),
                    subject,
                    keyPair.getPublic()
            ).build(signer);

            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(holder);

            writePem(certPath, cert);
            writePem(keyPath, keyPair.getPrivate());
            restrictPermissions(keyPath);

            log.debug("Generated self-signed certificate in {}", directory);
            return new CertificateManager(cert, keyPair.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate", e);
        }
    }

    /**
     * Builds a Netty {@link SslContext} configured for the Gemini protocol:
     * TLS 1.2+, with optional client certificate support.
     *
     * @return a configured {@link SslContext}
     */
    public SslContext sslContext() {
        try {
            return SslContextBuilder.forServer(privateKey, certificate)
                    .sslProvider(SslProvider.JDK)
                    .protocols("TLSv1.3", "TLSv1.2")
                    .clientAuth(ClientAuth.OPTIONAL)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("Failed to build SSL context", e);
        }
    }

    private static X509Certificate readCertificate(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path);
             PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            if (obj instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter().getCertificate(holder);
            }
            throw new IllegalArgumentException("Not a valid certificate: " + path);
        }
    }

    private static PrivateKey readPrivateKey(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path);
             PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (obj instanceof PEMKeyPair keyPair) {
                return converter.getKeyPair(keyPair).getPrivate();
            }
            if (obj instanceof PrivateKeyInfo keyInfo) {
                return converter.getPrivateKey(keyInfo);
            }
            throw new IllegalArgumentException("Not a valid private key: " + path);
        }
    }

    private static void writePem(Path path, Object obj) throws IOException {
        try (JcaPEMWriter writer = new JcaPEMWriter(Files.newBufferedWriter(path))) {
            writer.writeObject(obj);
        }
    }

    private static void restrictPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (e.g. Windows) — skip
        } catch (IOException e) {
            log.warn("Could not restrict permissions on {}", path);
        }
    }
}
