package uk.ac.staffs.leavebooking.identity.authservice;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {
    public static final String GOOGLE_FIREBASE_JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/"
                    + "securetoken@system.gserviceaccount.com";
    public static final String CREDENTIAL_PATH_REQUIRED =
            "FIREBASE_SERVICE_ACCOUNT_PATH is required when Firebase is enabled";
    public static final String CREDENTIAL_FILE_MISSING =
            "Firebase service-account file does not exist: ";
    public static final String PROJECT_MISMATCH =
            "Firebase service-account project does not match the configured project";

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        validateExpectedProject(properties.projectId());
        Path credentialPath = credentialPath(properties.serviceAccountPath());

        try (InputStream serviceAccount = Files.newInputStream(credentialPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            if (!(credentials instanceof ServiceAccountCredentials serviceCredentials)
                    || !properties.projectId().equals(serviceCredentials.getProjectId())) {
                throw new IllegalStateException(PROJECT_MISMATCH);
            }

            if (!FirebaseApp.getApps().isEmpty()) {
                FirebaseApp existing = FirebaseApp.getInstance();
                if (!properties.projectId().equals(existing.getOptions().getProjectId())) {
                    throw new IllegalStateException(PROJECT_MISMATCH);
                }
                return existing;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(properties.projectId())
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public JwtDecoder firebaseJwtDecoder(FirebaseProperties properties) {
        validateExpectedProject(properties.projectId());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_FIREBASE_JWK_SET_URI)
                .build();
        String issuer = "https://securetoken.google.com/" + properties.projectId();
        OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud",
                values -> values != null && values.contains(properties.projectId())
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTime, audience));
        return decoder;
    }

    private Path credentialPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(CREDENTIAL_PATH_REQUIRED);
        }
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(CREDENTIAL_FILE_MISSING + path);
        }
        return path;
    }

    private void validateExpectedProject(String projectId) {
        if (!FirebaseProperties.EXPECTED_PROJECT_ID.equals(projectId)) {
            throw new IllegalStateException(PROJECT_MISMATCH);
        }
    }
}
