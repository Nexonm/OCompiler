package autotest;

import codegen.JasminCodeGenerator;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ast.declarations.Program;
import semantic.visitors.ConstantFolder;
import semantic.visitors.DeadCodeReturnEliminator;
import semantic.visitors.SymbolTableBuilder;
import semantic.visitors.TypeChecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * Executes test cases by running them through the compiler pipeline
 */
public class TestRunner {

    /**
     * Runs all test cases and returns results
     */
    public List<TestCase> runTests(List<TestCase> testCases) {
        List<TestCase> results = new ArrayList<>();

        for (TestCase testCase : testCases) {
            TestCase result = runSingleTest(testCase);
            results.add(result);
        }

        return results;
    }

    /**
     * Runs a single test through the compiler pipeline
     */
    private TestCase runSingleTest(TestCase testCase) {
        try {
            String sourceCode = readFile(testCase.filePath().toString());

            // Stage 1: Lexer
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens;
            try {
                tokens = lexer.tokenize();
                if (!lexer.getErrors().isEmpty()){
                    String res = String.join("; ", lexer.getErrors());
                    return evaluateResult(testCase, TestCase.FailStage.LEXER, res);
                }
            } catch (Exception e) {
                return evaluateResult(testCase, TestCase.FailStage.LEXER, e.getMessage());
            }

            // Stage 2: Parser
            Parser parser = new Parser(tokens);
            Program program;
            try {
                program = parser.parse();
                if (!parser.getErrors().isEmpty()){
                    String res = String.join("; ", parser.getErrors());
                    return evaluateResult(testCase, TestCase.FailStage.PARSER, res);
                }
            } catch (Exception e) {
                return evaluateResult(testCase, TestCase.FailStage.PARSER, e.getMessage());
            }

            // Stage 3: Semantic Analysis
            try {
                SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
                symbolTableBuilder.analyze(program);
                if (!symbolTableBuilder.getErrors().isEmpty()){
                    String res = String.join("; ", symbolTableBuilder.getErrors());
                    return evaluateResult(testCase, TestCase.FailStage.SEMANTIC, res);
                }

                TypeChecker typeChecker = new TypeChecker(symbolTableBuilder.getGlobalScope());
                typeChecker.check(program);
                if (!typeChecker.getErrors().isEmpty()){
                    String res = String.join("; ", typeChecker.getErrors());
                    return evaluateResult(testCase, TestCase.FailStage.SEMANTIC, res);
                }

                DeadCodeReturnEliminator deadCodeElim = new DeadCodeReturnEliminator();
                deadCodeElim.optimize(program);
                int iteration = 0;
                while (true) {
                    iteration++;
                    ConstantFolder folder = new ConstantFolder();
                    boolean changed = folder.optimize(program);
                    if (!changed || folder.getExpressionsFolded() == 0) {
                        break;
                    }
                    if (iteration > 10) {
                        System.out.println("Warning: Constant folding exceeded 10 iterations");
                        break;
                    }
                }
            } catch (Exception e) {
                return evaluateResult(testCase, TestCase.FailStage.SEMANTIC, e.getMessage());
            }

            // Stage 4: Code Generation
            try {
                JasminCodeGenerator codeGen = new JasminCodeGenerator("");
                codeGen.generate(program);
            } catch (Exception e) {
                return evaluateResult(testCase, TestCase.FailStage.CODEGEN, e.getMessage());
            }

            // If we reach here, all stages passed
            return evaluateResult(testCase, TestCase.FailStage.NONE, null);

        } catch (IOException e) {
            return testCase.withResults(TestCase.TestStatus.ERROR, TestCase.FailStage.NONE,
                    "Failed to read test file: " + e.getMessage());
        }
    }

    /**
     * Evaluates whether the test result matches expectations
     */
    private TestCase evaluateResult(TestCase testCase, TestCase.FailStage actualFailStage, String errorMessage) {
        TestCase.TestStatus status;

        if (testCase.expectedResult() == TestCase.ExpectedResult.PASS) {
            // Test expects to pass
            if (actualFailStage == TestCase.FailStage.NONE) {
                status = TestCase.TestStatus.SUCCESSFUL;
            } else {
                status = TestCase.TestStatus.INCORRECT;
            }
        } else {
            // Test expects to fail
            if (actualFailStage == testCase.expectedFailStage()) {
                status = TestCase.TestStatus.SUCCESSFUL;
            } else if (actualFailStage == TestCase.FailStage.NONE) {
                status = TestCase.TestStatus.INCORRECT;
                errorMessage = "Expected to fail at " + testCase.expectedFailStage() + " but passed all stages";
            } else {
                status = TestCase.TestStatus.INCORRECT;
            }
        }

        return testCase.withResults(status, actualFailStage, errorMessage);
    }

    /**
     * Reads file content as a string
     */
    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}

