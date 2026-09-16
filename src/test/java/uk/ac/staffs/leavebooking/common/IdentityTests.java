package uk.ac.staffs.leavebooking.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unique identities")
class IdentityTests {
    private static final class TestContext {
    }

    @Test
    @DisplayName("An identity cannot be created with a null identifier")
    void nullIdentifierIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                Identity.<TestContext>of(null)
        );

        assertEquals(Identity.IDENTITY_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An identity cannot be created with an empty identifier")
    void emptyIdentifierIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                Identity.<TestContext>of("")
        );

        assertEquals(Identity.IDENTITY_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An identity cannot be created with a whitespace identifier")
    void whitespaceIdentifierIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                Identity.<TestContext>of("   ")
        );

        assertEquals(Identity.IDENTITY_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An identity can be reconstructed from a known identifier")
    void knownIdentifierIsReconstructed() {
        Identity<TestContext> identity = Identity.of("known-id");

        assertEquals("known-id", identity.id());
    }

    @Test
    @DisplayName("Identities with the same value are equal")
    void identitiesWithSameValueAreEqual() {
        Identity<TestContext> identity = Identity.of("known-id");
        Identity<TestContext> matchingIdentity = Identity.of("known-id");

        assertEquals(identity, matchingIdentity);
    }

    @Test
    @DisplayName("Identities with different values are not equal")
    void identitiesWithDifferentValuesAreNotEqual() {
        Identity<TestContext> identity = Identity.of("known-id");
        Identity<TestContext> differentIdentity = Identity.of("different-id");

        assertNotEquals(identity, differentIdentity);
    }

    @Test
    @DisplayName("Generated identity is a non-empty valid UUID")
    void generatedIdentityIsNonEmptyValidUuid() {
        Identity<TestContext> identity = Identity.generateId();

        UUID uuid = UUID.fromString(identity.id());

        assertFalse(identity.id().isBlank());
        assertEquals(identity.id(), uuid.toString());
    }

    @Test
    @DisplayName("Generated identities are different")
    void generatedIdentitiesAreDifferent() {
        Identity<TestContext> identity = Identity.generateId();
        Identity<TestContext> differentIdentity = Identity.generateId();

        assertNotEquals(identity, differentIdentity);
    }

    @Test
    @DisplayName("Generated identity uses UUID version 7")
    void generatedIdentityUsesUuidVersionSeven() {
        Identity<TestContext> identity = Identity.generateId();

        UUID uuid = UUID.fromString(identity.id());

        assertEquals(7, uuid.version());
    }
}
