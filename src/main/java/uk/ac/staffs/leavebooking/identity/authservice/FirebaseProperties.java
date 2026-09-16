package uk.ac.staffs.leavebooking.identity.authservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("firebase")
public record FirebaseProperties(
        boolean enabled,
        String projectId,
        String webApiKey,
        String serviceAccountPath
) {
    public static final String EXPECTED_PROJECT_ID = "leave-booking-system-f4e37";

    public FirebaseProperties {
        projectId = projectId == null || projectId.isBlank()
                ? EXPECTED_PROJECT_ID
                : projectId.trim();
        webApiKey = webApiKey == null ? "" : webApiKey.trim();
        serviceAccountPath = serviceAccountPath == null ? "" : serviceAccountPath.trim();
    }
}
