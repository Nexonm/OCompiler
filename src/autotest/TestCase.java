package autotest;

import java.nio.file.Path;

/**
 * Represents a single test case with metadata and execution results
 */
public record TestCase(
        Path filePath,
        String name,
        String description,
        ExpectedResult expectedResult,
        FailStage expectedFailStage,
        TestStatus status,
        FailStage actualFailStage,
        String errorMessage
) {

    public enum ExpectedResult {
        PASS, FAIL
    }

    public enum FailStage {
        LEXER, PARSER, SEMANTIC, CODEGEN, NONE
    }

    public enum TestStatus {
        NOT_RUN, SUCCESSFUL, INCORRECT, ERROR
    }

    /**
     * Creates a new TestCase with metadata only (before execution)
     */
    public static TestCase create(Path filePath, String name, String description,
                                  ExpectedResult expectedResult, FailStage expectedFailStage) {
        return new TestCase(filePath, name, description, expectedResult,
                expectedFailStage, TestStatus.NOT_RUN, FailStage.NONE, null);
    }

    /**
     * Creates a copy with execution results
     */
    public TestCase withResults(TestStatus status, FailStage actualFailStage, String errorMessage) {
        return new TestCase(filePath, name, description, expectedResult,
                expectedFailStage, status, actualFailStage, errorMessage);
    }

    /**
     * Checks if the test passed according to expected vs actual results
     */
    public boolean isSuccessful() {
        return status == TestStatus.SUCCESSFUL;
    }
}

