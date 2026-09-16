package uk.ac.staffs.leavebooking.identity.dto;

import java.util.List;

public record RoleCheckResponse(
        String uid,
        List<String> authorities
) {
    public RoleCheckResponse {
        authorities = List.copyOf(authorities);
    }
}
