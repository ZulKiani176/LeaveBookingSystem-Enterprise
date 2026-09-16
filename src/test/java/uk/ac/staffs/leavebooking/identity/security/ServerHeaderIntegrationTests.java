package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rabbitmq.enabled=false",
                "firebase.enabled=false",
                "rate-limit.enabled=false",
                "logging.file.name=target/test-logs/server-header.log"
        }
)
@DisplayName("Hidden server details")
class ServerHeaderIntegrationTests {
    @LocalServerPort private int port;

    @Test
    @DisplayName("The HTTP server does not reveal its software or version")
    void realEmbeddedServerDoesNotExposeTechnologyOrVersionHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/staff/staff-1")
                )
                .GET()
                .build();

        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("Server").isEmpty());
        assertTrue(response.headers().firstValue("X-Powered-By").isEmpty());
    }
}
