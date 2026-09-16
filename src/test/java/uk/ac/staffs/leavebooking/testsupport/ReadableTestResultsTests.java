package uk.ac.staffs.leavebooking.testsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Readable test results")
@ResourceLock(Resources.SYSTEM_OUT)
class ReadableTestResultsTests {
    @ParameterizedTest(name = "{displayName} (case {index})")
    @ValueSource(strings = {"PASS", "FAIL", "ABORTED", "SKIPPED"})
    @DisplayName("Each test outcome prints the correct result and a simple description")
    void printsTheActualOutcome(String result) {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getDisplayName()).thenReturn("A valid leave request is accepted");
        ReadableTestResults listener = new ReadableTestResults();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            switch (result) {
                case "PASS" -> listener.testSuccessful(context);
                case "FAIL" -> listener.testFailed(context, new AssertionError("Private detail"));
                case "ABORTED" -> listener.testAborted(context, new RuntimeException("Private detail"));
                case "SKIPPED" -> listener.testDisabled(context, Optional.of("Private detail"));
                default -> throw new IllegalArgumentException(result);
            }
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(result + ": A valid leave request is accepted" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }
}
