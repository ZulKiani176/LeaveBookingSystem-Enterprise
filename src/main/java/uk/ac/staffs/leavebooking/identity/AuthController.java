package uk.ac.staffs.leavebooking.identity;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.identity.authservice.FirebaseAuthService;
import uk.ac.staffs.leavebooking.identity.dto.LoginRequest;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.dto.RegisterRequest;
import uk.ac.staffs.leavebooking.identity.dto.RegisterResponse;
import uk.ac.staffs.leavebooking.identity.dto.RoleCheckResponse;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final FirebaseAuthService firebaseAuthService;

    public AuthController(FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return firebaseAuthService.loginUser(request.email(), request.password());
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        LOG.info("Registering Firebase user email={} role={}", request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(firebaseAuthService.registerUser(request));
    }

    @GetMapping("/role-check")
    @PreAuthorize("isAuthenticated()")
    public RoleCheckResponse roleCheck(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();
        return new RoleCheckResponse(authentication.getName(), authorities);
    }
}
