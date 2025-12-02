package autotest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans the tests directory recursively to find all test files matching the pattern
 */
public class TestScanner {

    private static final Pattern TEST_FILE_PATTERN = Pattern.compile("test.*\\.o");

    // Metadata parsing patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("//\\s*Name:\\s*(.*)");
    private static final Pattern DESC_PATTERN = Pattern.compile("//\\s*Description:\\s*(.*)");
    private static final Pattern EXPECTED_RESULT_PATTERN = Pattern.compile("//\\s*Expected result:\\s*(pass|fail)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPECTED_FAIL_STAGE_PATTERN = Pattern.compile("//\\s*Expected fail stage:\\s*(lexer|parser|semantic|codegen|none)", Pattern.CASE_INSENSITIVE);

    private final Path testsDirectory;

    public TestScanner(String testsDirectoryPath) {
        this.testsDirectory = Paths.get(testsDirectoryPath);
    }

    /**
     * Scans the tests directory recursively and returns all discovered test cases
     */
    public List<TestCase> scanTests() throws IOException {
        List<TestCase> testCases = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(testsDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> TEST_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                    .forEach(path -> {
                        try {
                            TestCase testCase = parseTestFile(path);
                            testCases.add(testCase);
                        } catch (IOException e) {
                            System.err.println("Error parsing test file: " + path + " - " + e.getMessage());
                        }
                    });
        }

        return testCases;
    }

    /**
     * Parses a test file to extract metadata from the header comments
     */
    private TestCase parseTestFile(Path filePath) throws IOException {
        String name = filePath.getFileName().toString();
        String description = "";
        TestCase.ExpectedResult expectedResult = TestCase.ExpectedResult.PASS;
        TestCase.FailStage expectedFailStage = TestCase.FailStage.NONE;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            int lineCount = 0;

            // Read first 10 lines to extract metadata
            while ((line = reader.readLine()) != null && lineCount < 10) {
                lineCount++;

                Matcher nameMatcher = NAME_PATTERN.matcher(line);
                if (nameMatcher.find()) {
                    name = nameMatcher.group(1).trim();
                    continue;
                }

                Matcher descMatcher = DESC_PATTERN.matcher(line);
                if (descMatcher.find()) {
                    description = descMatcher.group(1).trim();
                    continue;
                }

                Matcher resultMatcher = EXPECTED_RESULT_PATTERN.matcher(line);
                if (resultMatcher.find()) {
                    expectedResult = resultMatcher.group(1).equalsIgnoreCase("pass")
                            ? TestCase.ExpectedResult.PASS
                            : TestCase.ExpectedResult.FAIL;
                    continue;
                }

                Matcher stageMatcher = EXPECTED_FAIL_STAGE_PATTERN.matcher(line);
                if (stageMatcher.find()) {
                    String stage = stageMatcher.group(1).toUpperCase();
                    expectedFailStage = TestCase.FailStage.valueOf(stage);
                }
            }
        }

        return TestCase.create(filePath, name, description, expectedResult, expectedFailStage);
    }
}

