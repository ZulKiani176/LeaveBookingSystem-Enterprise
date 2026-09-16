package uk.ac.staffs.leavebooking.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("localId") String uid,
        String email,
        @JsonProperty("displayName") String username,
        @JsonProperty("idToken") String accessToken,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String refreshToken,
        @JsonProperty("expiresIn") String expiresInSeconds
) {
}
