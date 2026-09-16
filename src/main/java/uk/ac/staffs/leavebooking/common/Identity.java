package uk.ac.staffs.leavebooking.common;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;

public final class Identity<T> implements ValueObject {
    public static final String IDENTITY_NOT_EMPTY = "Identity value cannot be empty";

    private final String id;

    private Identity(String id) {
        this.id = argumentNotEmpty(id, IDENTITY_NOT_EMPTY);
    }

    public String id() {
        return id;
    }

    public static <T> Identity<T> of(String id) {
        return new Identity<>(id);
    }

    public static <T> Identity<T> generateId() {
        String id = UUID.ofEpochMillis(Instant.now().toEpochMilli()).toString();
        return new Identity<>(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Identity<?> identity)) {
            return false;
        }
        return Objects.equals(id, identity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
