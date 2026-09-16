package uk.ac.staffs.leavebooking.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Staff names")
class FullNameTests {
    @Test
    @DisplayName("A valid full name is created successfully")
    void validFullNameIsCreatedSuccessfully() {
        FullName fullName = new FullName("Ada", "Lovelace");

        assertNotNull(fullName);
        assertEquals("Ada", fullName.firstName());
        assertEquals("Lovelace", fullName.surname());
    }

    @Test
    @DisplayName("Surrounding whitespace is trimmed from both name parts")
    void surroundingWhitespaceIsTrimmed() {
        FullName fullName = new FullName("  Ada  ", "  Lovelace  ");

        assertEquals("Ada", fullName.firstName());
        assertEquals("Lovelace", fullName.surname());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("A null, empty or blank first name is rejected")
    void invalidFirstNameIsRejected(String invalidFirstName) {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new FullName(invalidFirstName, "Lovelace")
        );

        assertEquals(FullName.FIRST_NAME_NOT_EMPTY, exception.getMessage());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("A null, empty or blank surname is rejected")
    void invalidSurnameIsRejected(String invalidSurname) {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new FullName("Ada", invalidSurname)
        );

        assertEquals(FullName.SURNAME_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("Full names with the same state are equal")
    void fullNamesWithTheSameStateAreEqual() {
        FullName first = new FullName("Ada", "Lovelace");
        FullName second = new FullName("Ada", "Lovelace");

        assertEquals(first, second);
    }

    @Test
    @DisplayName("A first name at the lecturer-defined maximum length is valid")
    void maximumLengthFirstNameIsValid() {
        FullName fullName = new FullName("a".repeat(FullName.MAX_FIRST_NAME_LENGTH), "Lovelace");

        assertEquals(FullName.MAX_FIRST_NAME_LENGTH, fullName.firstName().length());
    }

    @Test
    @DisplayName("A first name above the lecturer-defined maximum length is rejected")
    void firstNameAboveMaximumLengthIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new FullName("a".repeat(FullName.MAX_FIRST_NAME_LENGTH + 1), "Lovelace")
        );

        assertEquals(FullName.FIRST_NAME_LENGTH, exception.getMessage());
    }

    @Test
    @DisplayName("A surname at the lecturer-defined maximum length is valid")
    void maximumLengthSurnameIsValid() {
        FullName fullName = new FullName("Ada", "a".repeat(FullName.MAX_SURNAME_LENGTH));

        assertEquals(FullName.MAX_SURNAME_LENGTH, fullName.surname().length());
    }

    @Test
    @DisplayName("A surname above the lecturer-defined maximum length is rejected")
    void surnameAboveMaximumLengthIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new FullName("Ada", "a".repeat(FullName.MAX_SURNAME_LENGTH + 1))
        );

        assertEquals(FullName.SURNAME_LENGTH, exception.getMessage());
    }
}
