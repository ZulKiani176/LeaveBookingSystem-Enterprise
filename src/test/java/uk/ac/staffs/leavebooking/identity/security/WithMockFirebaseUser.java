package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithMockFirebaseUserSecurityContextFactory.class)
public @interface WithMockFirebaseUser {
    String uid() default "firebase-test-user";

    Role role() default Role.ADMIN;

    String staffId() default "staff-1";
}
