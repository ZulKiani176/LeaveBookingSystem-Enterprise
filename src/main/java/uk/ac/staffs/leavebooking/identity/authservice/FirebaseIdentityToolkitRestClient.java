package uk.ac.staffs.leavebooking.identity.authservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.exceptions.AuthenticationFailedException;
import uk.ac.staffs.leavebooking.identity.exceptions.FirebaseConfigurationException;

import java.util.Map;

@Component
public class FirebaseIdentityToolkitRestClient implements FirebaseIdentityToolkitClient {
    private static final Logger LOG =
            LoggerFactory.getLogger(FirebaseIdentityToolkitRestClient.class);
    private static final String BASE_URL = "https://identitytoolkit.googleapis.com";

    private final FirebaseProperties properties;
    private final RestClient restClient;

    public FirebaseIdentityToolkitRestClient(FirebaseProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Override
    public LoginResponse login(String email, String password) {
        if (!properties.enabled()) {
            throw new FirebaseConfigurationException(FirebaseAuthService.FIREBASE_DISABLED);
        }
        if (properties.webApiKey().isBlank()) {
            throw new FirebaseConfigurationException(FirebaseAuthService.WEB_API_KEY_REQUIRED);
        }
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        );
        try {
            return restClient.post()
                    .uri(builder -> builder
                            .path("/v1/accounts:signInWithPassword")
                            .queryParam("key", properties.webApiKey())
                            .build())
                    .body(body)
                    .retrieve()
                    .body(LoginResponse.class);
        } catch (RestClientResponseException exception) {
            LOG.warn("Firebase login rejected for {} with status {}",
                    email, exception.getStatusCode().value());
            throw new AuthenticationFailedException(FirebaseAuthService.AUTHENTICATION_FAILED);
        } catch (RestClientException exception) {
            LOG.error("Firebase login request failed before an authentication response was received");
            throw new AuthenticationFailedException(FirebaseAuthService.AUTHENTICATION_FAILED);
        }
    }
}
