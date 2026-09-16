package uk.ac.staffs.leavebooking.common.events.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.staffs.leavebooking.common.events.RemoteEvent;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HR Staff Integration Event")
class HrStaffIntegrationEventTests {
    private static final LocalDate OCCURRED_ON = LocalDate.of(2026, 8, 25);

    @Test
    @DisplayName("HR staff-created events retain immutable external snapshot values")
    void createdEventRetainsSnapshot() {
        HrStaffMemberCreatedIntegrationEvent event = createdEvent(41L, "ACTIVE");

        assertEquals(41L, event.id());
        assertEquals("hr-staff-1", event.staffMemberId());
        assertEquals("Ada", event.firstName());
        assertEquals("Lovelace", event.surname());
        assertEquals("ada@example.com", event.email());
        assertEquals("ACTIVE", event.employmentStatus());
        assertTrue(event instanceof RemoteEvent);
    }

    @Test
    @DisplayName("HR event withId returns an immutable copy carrying the replacement identity")
    void withIdReturnsCopy() {
        HrStaffMemberCreatedIntegrationEvent original = createdEvent(41L, "ACTIVE");

        HrStaffMemberCreatedIntegrationEvent copy = original.withId(42L);

        assertNotSame(original, copy);
        assertEquals(41L, original.id());
        assertEquals(42L, copy.id());
        assertEquals(original.staffMemberId(), copy.staffMemberId());
    }

    @Test
    @DisplayName("Personal-details events contain only identity, name and email update data")
    void personalDetailsContractIsDeliberatelyNarrow() {
        var componentNames = Arrays.stream(
                        HrStaffPersonalDetailsUpdatedIntegrationEvent.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .toList();

        assertEquals(
                java.util.List.of("id", "occurredOn", "staffMemberId", "firstName", "surname", "email"),
                componentNames
        );
    }

    @Test
    @DisplayName("Common HR contracts expose employment status as String without Staff types")
    void commonContractDoesNotDependOnStaffDomain() {
        RecordComponent status = Arrays.stream(
                        HrStaffMemberCreatedIntegrationEvent.class.getRecordComponents()
                )
                .filter(component -> component.getName().equals("employmentStatus"))
                .findFirst()
                .orElseThrow();

        assertEquals(String.class, status.getType());
        assertTrue(Arrays.stream(HrStaffMemberCreatedIntegrationEvent.class.getDeclaredFields())
                .noneMatch(field -> field.getType().getPackageName().contains(".staff.")));
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("HR staff-created events reject a missing external staff identity")
    void createdEventRejectsMissingStaffIdentity(String staffMemberId) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new HrStaffMemberCreatedIntegrationEvent(
                        41L, OCCURRED_ON, staffMemberId, "Ada", "Lovelace", "ada@example.com",
                        LocalDate.of(2024, 1, 1), "Engineering", "manager-1", "Developer",
                        LocalDate.of(2024, 1, 1), "Senior", "Permanent", "ACTIVE"
                )
        );

        assertEquals(HrStaffMemberCreatedIntegrationEvent.STAFF_MEMBER_ID_NOT_EMPTY,
                exception.getMessage());
    }

    @Test
    @DisplayName("External HR events require a source event identity for idempotency")
    void externalEventRequiresIdentity() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createdEvent(null, "ACTIVE")
        );

        assertEquals(HrStaffMemberCreatedIntegrationEvent.ID_NOT_NULL, exception.getMessage());
    }

    private HrStaffMemberCreatedIntegrationEvent createdEvent(Long id, String status) {
        return new HrStaffMemberCreatedIntegrationEvent(
                id,
                OCCURRED_ON,
                "  hr-staff-1  ",
                "  Ada  ",
                "  Lovelace  ",
                "  ada@example.com  ",
                LocalDate.of(2024, 1, 1),
                "  Engineering  ",
                "  manager-1  ",
                "  Developer  ",
                LocalDate.of(2024, 1, 1),
                "  Senior  ",
                "  Permanent  ",
                status
        );
    }
}
