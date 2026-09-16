package uk.ac.staffs.leavebooking.identity.authservice;

import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;

public interface FirebaseIdentityToolkitClient {
    LoginResponse login(String email, String password);
}
