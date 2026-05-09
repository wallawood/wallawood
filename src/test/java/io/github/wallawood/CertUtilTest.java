package io.github.wallawood;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CertUtilTest {

    @Test
    void cnExtractsCommonName() throws Exception {
        X509Certificate cert = selfSigned("CN=alice");
        assertEquals("alice", CertUtil.cn(cert));
    }

    @Test
    void cnHandlesMultiPartDn() throws Exception {
        X509Certificate cert = selfSigned("CN=bob,O=Example,C=US");
        assertEquals("bob", CertUtil.cn(cert));
    }

    @Test
    void cnReturnsDnWhenNoCn() throws Exception {
        X509Certificate cert = selfSigned("O=NoCN");
        String result = CertUtil.cn(cert);
        assertFalse(result.startsWith("CN="));
        assertTrue(result.contains("NoCN"));
    }

    @Test
    void fingerprintReturnsHexString() throws Exception {
        X509Certificate cert = selfSigned("CN=test");
        String fp = CertUtil.fingerprint(cert);
        assertNotNull(fp);
        assertEquals(64, fp.length()); // SHA-256 = 32 bytes = 64 hex chars
        assertTrue(fp.matches("[0-9a-f]+"));
    }

    @Test
    void fingerprintIsDeterministic() throws Exception {
        X509Certificate cert = selfSigned("CN=test");
        assertEquals(CertUtil.fingerprint(cert), CertUtil.fingerprint(cert));
    }

    @Test
    void differentCertsHaveDifferentFingerprints() throws Exception {
        X509Certificate a = selfSigned("CN=a");
        X509Certificate b = selfSigned("CN=b");
        assertNotEquals(CertUtil.fingerprint(a), CertUtil.fingerprint(b));
    }

    private static X509Certificate selfSigned(String dn) throws Exception {
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        var kp = kpg.generateKeyPair();
        var name = new X500Name(dn);
        var now = Instant.now();
        var builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.valueOf(now.toEpochMilli()),
                Date.from(now), Date.from(now.plusSeconds(3600)),
                name, kp.getPublic());
        var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
