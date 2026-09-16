package uk.ac.staffs.leavebooking.identity.dto;

public record RegisterResponse(
        String uid,
        String email,
        String username,
        String message
) {
}
