package uk.ac.staffs.leavebooking.common;

import java.util.Objects;

public abstract class Entity<T> {
    public static final String IDENTITY_CANNOT_BE_NULL = "Identity cannot be null";

    protected final Identity<T> id;

    protected Entity(Identity<T> id) {
        this.id = DomainAssertions.argumentNotNull(id, IDENTITY_CANNOT_BE_NULL);
    }

    public Identity<T> id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Entity<?> entity)) {
            return false;
        }
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
