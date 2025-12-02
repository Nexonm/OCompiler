package autotest;

import java.util.List;

/**
 * Prints test execution results with statistics and details
 */
public class TestResultPrinter {

    /**
     * Prints comprehensive test results
     */
    public void printResults(List<TestCase> results) {
        int total = results.size();
        int successful = (int) results.stream().filter(TestCase::isSuccessful).count();
        int incorrect = (int) results.stream().filter(tc -> tc.status() == TestCase.TestStatus.INCORRECT).count();
        int errors = (int) results.stream().filter(tc -> tc.status() == TestCase.TestStatus.ERROR).count();

        printHeader();
        printStatistics(total, successful, incorrect, errors);
        printDetailedResults(results);
        printFooter(successful, total);
    }

    private void printHeader() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                          TEST EXECUTION RESULTS");
        System.out.println("=".repeat(80));
    }

    private void printStatistics(int total, int successful, int incorrect, int errors) {
        System.out.println("\nSTATISTICS:");
        System.out.println("─".repeat(80));
        System.out.printf("  Total Tests:       %d%n", total);
        System.out.printf("  ✓ Successful:      %d (%.1f%%)%n", successful, (successful * 100.0 / total));
        System.out.printf("  ✗ Incorrect:       %d (%.1f%%)%n", incorrect, (incorrect * 100.0 / total));
        System.out.printf("  ⚠ Errors:          %d (%.1f%%)%n", errors, (errors * 100.0 / total));
        System.out.println("─".repeat(80));
    }

    private void printDetailedResults(List<TestCase> results) {
        List<TestCase> failedTests = results.stream()
                .filter(tc -> !tc.isSuccessful())
                .toList();

        if (failedTests.isEmpty()) {
            System.out.println("\nAll tests passed successfully!");
            return;
        }

        System.out.println("\nFAILED TESTS DETAILS:");
        System.out.println("─".repeat(80));

        for (int i = 0; i < failedTests.size(); i++) {
            TestCase tc = failedTests.get(i);
            System.out.printf("%n[%d] %s%n", i + 1, tc.name());
            System.out.println("    Path:                 " + tc.filePath());
            System.out.println("    Description:          " + (tc.description().isEmpty() ? "N/A" : tc.description()));
            System.out.println("    Expected Result:      " + tc.expectedResult());

            if (tc.expectedResult() == TestCase.ExpectedResult.FAIL) {
                System.out.println("    Expected Fail Stage:  " + tc.expectedFailStage());
            }

            System.out.println("    Actual Fail Stage:    " +
                    (tc.actualFailStage() == TestCase.FailStage.NONE ? "PASSED" : tc.actualFailStage()));

            if (tc.errorMessage() != null) {
                System.out.println("    Error Message:        " + tc.errorMessage());
            }

            System.out.println("    Status:               " + getStatusSymbol(tc.status()) + " " + tc.status());

            if (i < failedTests.size() - 1) {
                System.out.println("    " + "─".repeat(76));
            }
        }
    }

    private void printFooter(int successful, int total) {
        System.out.println("\n" + "=".repeat(80));
        if (successful == total) {
            System.out.println("                    ✓ ALL TESTS PASSED ✓");
        } else {
            System.out.printf("                    %d/%d TESTS PASSED%n", successful, total);
        }
        System.out.println("=".repeat(80) + "\n");
    }

    private String getStatusSymbol(TestCase.TestStatus status) {
        return switch (status) {
            case SUCCESSFUL -> "✓";
            case INCORRECT -> "✗";
            case ERROR -> "⚠";
            case NOT_RUN -> "○";
        };
    }
}

