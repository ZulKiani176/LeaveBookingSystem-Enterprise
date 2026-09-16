package uk.ac.staffs.leavebooking.testsupport;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

public class ReadableTestResults implements TestWatcher {
    @Override
    public void testSuccessful(ExtensionContext context) {
        report("PASS", context);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        report("FAIL", context);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        report("ABORTED", context);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        report("SKIPPED", context);
    }

    private static void report(String result, ExtensionContext context) {
        System.out.println(result + ": " + context.getDisplayName());
    }
}
