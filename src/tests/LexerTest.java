package tests;

import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;

import java.util.List;

public class LexerTest {

    public static void main(String[] args) {
        System.out.println("Running Lexer Tests...");
        testAllTokens();
        testLiterals();
        testComments();
        testErrorHandling();
        testGeneralScenarios();
        System.out.println("Lexer Tests Finished.");
    }

    private static void testAllTokens() {
        System.out.println("--- Testing All Token Types ---");
        String source = "class extends is end var method this if then else while loop return true false := => ( ) [ ] { } , . :";
        runTest("All Tokens", source);
    }

    private static void testLiterals() {
        System.out.println("--- Testing Literals ---");
        String source = """
                var intVar: Integer(123)
                var realVar: Real(123.45)
                var boolVar: Boolean(true)
                var stringVar: String("hello world")
                """;
        runTest("Literals", source);
    }

    private static void testComments() {
        System.out.println("--- Testing Comments ---");
        String source = """
                // This is a single-line comment.
                var x: Integer(1) // This is another comment.
                /* This is a block comment.
                   It can span multiple lines. */
                var y: Integer(2) /* block comment on same line */
                """;
        runTest("Comments", source);
    }

    private static void testErrorHandling() {
        System.out.println("--- Testing Error Handling ---");
        runTest("Unterminated String", "\"hello");
        runTest("Unterminated Block Comment", "/* hello");
        runTest("Invalid Character", "var a: Integer(1) @");
    }

    private static void testGeneralScenarios() {
        System.out.println("--- Testing General Scenarios from Test Cases (General).md ---");

        String test1 = """
                class SimpleClass is
                    var value: Integer(42)

                    this() is
                    end
                end
                """;
        runTest("Test 1: Basic Class Declaration", test1);

        String test5 = """
                class LoopTest is
                    method factorial(n: Integer) : Integer is
                        var result: Integer(1)
                        var i: Integer(1)
                        while i.LessEqual(n) loop
                            result := result.Mult(i)
                            i := i.Plus(Integer(1))
                        end
                        return result
                    end

                    this() is
                    end
                end
                """;
        runTest("Test 5: Loop", test5);

        String test12 = """
                class TypeErrorTest is
                    method getNumber() : Integer is
                        return Boolean(true) // return Boolean instead of Integer
                    end

                    this() is
                    end
                end
                """;
        runTest("Error Test 12: Wrong Return Type", test12);
    }

    private static void runTest(String testName, String source) {
        System.out.println("\n--- Running Test: " + testName + " ---");
        System.out.println("Source:\n" + source);
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Tokens:");
        TokenPrinter printer = new TokenPrinter(tokens, lexer.getErrors(), source);
        printer.printTokens();

        List<String> errors = lexer.getErrors();
        if (!errors.isEmpty()) {
            System.out.println("Lexer Errors:");
            for (String error : errors) {
                System.out.println(error);
            }
        }
        System.out.println("--- Test " + testName + " Finished ---");
    }
}
