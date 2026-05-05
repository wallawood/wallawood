package io.gemboot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

/**
 * Utility methods for working with TLS client certificates in Gemini applications.
 *
 * <pre>{@code
 * @Path("/whoami")
 * public GeminiResponse whoami(@Context X509Certificate cert) {
 *     String name = CertUtil.cn(cert);
 *     String id = CertUtil.fingerprint(cert);
 *     return GeminiResponse.success("Hello, " + name + " (" + id + ")");
 * }
 * }</pre>
 */
public final class CertUtil {

    private CertUtil() {}

    /**
     * Extracts the Common Name (CN) from the certificate's subject.
     * Returns the raw subject DN if it does not start with {@code "CN="}.
     *
     * @param cert the certificate
     * @return the CN value
     */
    public static String cn(X509Certificate cert) {
        String dn = cert.getSubjectX500Principal().getName();
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return dn;
    }

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    /**
     * Returns the SHA-256 fingerprint of the certificate as a lowercase hex string.
     * This is a globally unique identifier for the certificate, suitable for
     * database keys or TOFU (Trust on First Use) identity tracking.
     *
     * @param cert the certificate
     * @return the hex-encoded SHA-256 fingerprint
     */
    public static String fingerprint(X509Certificate cert) {
        try {
            MessageDigest md = SHA256.get();
            md.reset();
            byte[] digest = md.digest(cert.getEncoded());
            return HexFormat.of().formatHex(digest);
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Failed to compute certificate fingerprint", e);
        }
    }
}
