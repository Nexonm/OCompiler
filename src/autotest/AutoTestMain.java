package autotest;

import java.io.IOException;
import java.util.List;

/**
 * Main entry point for the auto-testing system
 */
public class AutoTestMain {

    private static final String TESTS_DIRECTORY = "./src/tests";

    public static void main(String[] args) {
        String testsDir = args.length > 0 ? args[0] : TESTS_DIRECTORY;

        try {
            System.out.println("Scanning for test files in: " + testsDir);

            // Step 1: Scan for test files
            TestScanner scanner = new TestScanner(testsDir);
            List<TestCase> testCases = scanner.scanTests();

            System.out.println("Found " + testCases.size() + " test files\n");

            if (testCases.isEmpty()) {
                System.out.println("No test files found matching pattern 'test*.txt'");
                return;
            }

            // Step 2: Run tests
            System.out.println("Running tests...\n");
            TestRunner runner = new TestRunner();
            List<TestCase> results = runner.runTests(testCases);

            // Step 3: Print results
            TestResultPrinter printer = new TestResultPrinter();
            printer.printResults(results);

        } catch (IOException e) {
            System.err.println("Error during test execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

