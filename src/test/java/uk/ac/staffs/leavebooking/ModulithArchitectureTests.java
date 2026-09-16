package uk.ac.staffs.leavebooking;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

@DisplayName("Module boundaries")
class ModulithArchitectureTests {
    @Test
    @DisplayName("Modules use only the interfaces they are allowed to access")
    void moduleDependenciesRespectExposedInterfaces() {
        ApplicationModules.of(LeaveBookingSystemApplication.class).verify();
    }
}
