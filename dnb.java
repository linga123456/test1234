<!-- BouncyCastle for PEM parsing -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk15on</artifactId>
    <version>1.70</version>
</dependency>

<!-- HttpClient and Spring Web -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.1</version>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>5.3.21</version>
</dependency>



import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        // Paths to the certificate, key, and CA files
        String certPath = "path/to/cert.pem";
        String keyPath = "path/to/key.pem";
        String caCertPath = "path/to/ca-cert.pem";

        // Create SSL context with PEM-based client cert, key, and CA
        SSLContext sslContext = createSslContext(certPath, keyPath, caCertPath);

        // Optionally, you can disable hostname verification (use in testing only)
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)  // Skip hostname verification
                .build();

        // Create RestTemplate using the custom HTTP client
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    private SSLContext createSslContext(String certPath, String keyPath, String caCertPath) throws Exception {
        // Load client certificate from PEM file
        X509Certificate clientCert = loadCertificateFromPEM(certPath);

        // Load private key from PEM file
        PrivateKey privateKey = loadPrivateKeyFromPEM(keyPath);

        // Load CA certificate from PEM file
        X509Certificate caCert = loadCertificateFromPEM(caCertPath);

        // Create KeyStore and load client certificate and private key
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);  // Initialize KeyStore
        keyStore.setKeyEntry("client-cert", privateKey, null, new java.security.cert.Certificate[]{clientCert});

        // Create another KeyStore for CA certificates
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);  // Initialize KeyStore
        trustStore.setCertificateEntry("ca-cert", caCert);  // Load CA cert

        // Create the SSLContext using both the keyStore (client cert) and trustStore (CA cert)
        return SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, null)  // Load client cert and private key
                .loadTrustMaterial(trustStore, null)  // Load CA cert to trust the server
                .build();
    }

    // Helper method to load X.509 certificate from PEM file
    private X509Certificate loadCertificateFromPEM(String certPath) throws Exception {
        try (InputStream certInputStream = new FileInputStream(certPath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(certInputStream);
        }
    }

    // Helper method to load private key from PEM file (supports both PKCS#8 and PKCS#1 formats)
    private PrivateKey loadPrivateKeyFromPEM(String keyPath) throws Exception {
        try (Reader keyReader = new FileReader(keyPath);
             PEMParser pemParser = new PEMParser(keyReader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object instanceof PEMKeyPair) {
                // Handle OpenSSL-style PEM with RSA/EC private keys
                return converter.getKeyPair((PEMKeyPair) object).getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                // Handle PKCS#8 format
                return converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Invalid PEM file format for private key");
            }
        }
    }
}

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class PrivateKeyLoader {

    public static void main(String[] args) {
        try {
            // Load the private key from the .key file in the resources folder
            InputStream keyStream = PrivateKeyLoader.class.getClassLoader().getResourceAsStream("private.key");

            // Ensure the key file was found
            if (keyStream == null) {
                throw new IllegalArgumentException("Private key file not found in resources folder.");
            }

            // Read the private key file (PEM format) as a string
            BufferedReader reader = new BufferedReader(new InputStreamReader(keyStream, StandardCharsets.UTF_8));
            StringBuilder keyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("-----")) {
                    keyBuilder.append(line);  // Ignore the PEM headers and footers
                }
            }
            reader.close();

            // Decode the Base64 encoded key
            byte[] keyBytes = Base64.getDecoder().decode(keyBuilder.toString());

            // Create a KeyFactory for the appropriate algorithm (e.g., RSA)
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // Create the private key from the key bytes
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Now you have the PrivateKey object
            System.out.println("Private Key Algorithm: " + privateKey.getAlgorithm());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

