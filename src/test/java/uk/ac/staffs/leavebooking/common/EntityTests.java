package uk.ac.staffs.leavebooking.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Identity-based equality")
class EntityTests {
    @Test
    @DisplayName("An entity cannot be created without an identity")
    void nullIdentityIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new TestEntity(null, "first name")
        );

        assertEquals(Entity.IDENTITY_CANNOT_BE_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("An entity exposes its identity")
    void entityExposesItsIdentity() {
        Identity<TestEntity> identity = Identity.of("known-id");
        TestEntity entity = new TestEntity(identity, "first name");

        assertEquals(identity, entity.id());
    }

    @Test
    @DisplayName("Entities of the same type with the same identity are equal")
    void sameTypeEntitiesWithSameIdentityAreEqual() {
        Identity<TestEntity> identity = Identity.of("known-id");
        TestEntity entity = new TestEntity(identity, "first name");
        TestEntity matchingEntity = new TestEntity(identity, "different name");

        assertEquals(entity, matchingEntity);
    }

    @Test
    @DisplayName("Entities with different identities are not equal")
    void entitiesWithDifferentIdentitiesAreNotEqual() {
        TestEntity entity = new TestEntity(Identity.of("known-id"), "first name");
        TestEntity differentEntity = new TestEntity(Identity.of("different-id"), "first name");

        assertNotEquals(entity, differentEntity);
    }

    private static final class TestEntity extends Entity<TestEntity> {
        private final String name;

        private TestEntity(Identity<TestEntity> id, String name) {
            super(id);
            this.name = name;
        }

        String name() {
            return name;
        }
    }
}
