import lexer.Lexer;
import lexer.Token;
import parser.ASTTreePrinter;
import parser.Parser;
import parser.ast.declarations.Program;

import java.util.List;

public class ParserTest {

    public static void main(String[] args) {
        System.out.println("Running Parser Tests...");
        testDeclarations();
        testStatements();
        testExpressions();
        testSyntaxErrors();
        testGeneralScenarios();
        System.out.println("Parser Tests Finished.");
    }

    private static void testDeclarations() {
        System.out.println("--- Testing Declarations ---");
        String classDecl = "class MyClass extends BaseClass is end";
        runTest("Class Declaration", classDecl);

        String varDecl = """
                class MyClass is
                    var x: Integer(1)
                end
                """;
        runTest("Variable Declaration", varDecl);

        String methodDecl = """
                class MyClass is
                    method myMethod(a: Type) : ReturnType is
                    end
                end
                """;
        runTest("Method Declaration", methodDecl);

        String constructorDecl = """
                class MyClass is
                    this(a: Type) is
                    end
                end
                """;
        runTest("Constructor Declaration", constructorDecl);
    }

    private static void testStatements() {
        System.out.println("--- Testing Statements ---");
        String ifStatement = """
                class MyClass is
                    method myMethod is
                        if true then
                        end
                    end
                end
                """;
        runTest("If Statement", ifStatement);

        String whileStatement = """
                class MyClass is
                    method myMethod is
                        while true loop
                        end
                    end
                end
                """;
        runTest("While Statement", whileStatement);

        String returnStatement = """
                class MyClass is
                    method myMethod is
                        return
                    end
                end
                """;
        runTest("Return Statement", returnStatement);

        String assignmentStatement = """
                class MyClass is
                    method myMethod is
                        x := 1
                    end
                end
                """;
        runTest("Assignment Statement", assignmentStatement);
    }

    private static void testExpressions() {
        System.out.println("--- Testing Expressions ---");
        String chainedCall = """
                class MyClass is
                    method myMethod is
                        var a: obj.method1().method2()
                    end
                end
                """;
        runTest("Chained Method Call", chainedCall);
    }

    private static void testSyntaxErrors() {
        System.out.println("--- Testing Syntax Errors ---");
        String missingEnd = "class MyClass is";
        runTest("Missing 'end'", missingEnd);

        String misplacedElse = """
                class MyClass is
                    method myMethod is
                        else
                    end
                end
                """;
        runTest("Misplaced 'else'", misplacedElse);

        String invalidExpression = """
                class MyClass is
                    method myMethod is
                        x := 1 + 2
                    end
                end
                """;
        runTest("Invalid Expression", invalidExpression);
    }

    private static void testGeneralScenarios() {
        System.out.println("--- Testing General Scenarios from Test Cases (General).md ---");

        String test2 = """
                class Base is
                    var x: Integer(10)
                    method getValue() : Integer is
                        return x
                    end
                    this() is
                    end
                end
                class Derived extends Base is
                    var y: Integer(20)
                    this() is
                    end
                end
                """;
        runTest("Test 2: Basic Inheritance", test2);

        String test4 = """
                class ConditionalTest is
                    method test(x: Integer) : Integer is
                        if x.Greater(Integer(5)) then
                            return Integer(1)
                        else
                            return Integer(0)
                        end
                    end
                    this() is
                        var result: test(Integer(10))
                    end
                end
                """;
        runTest("Test 4: If-Else", test4);
    }

    private static void runTest(String testName, String source) {
        System.out.println("\n--- Running Test: " + testName + " ---");
        System.out.println("Source:\n" + source);
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        if (!lexer.getErrors().isEmpty()) {
            System.out.println("Lexer Errors found, skipping parsing:");
            lexer.getErrors().forEach(System.out::println);
            System.out.println("--- Test " + testName + " Finished ---");
            return;
        }

        Parser parser = new Parser(tokens);
        Program program = parser.parse();

        System.out.println("AST:");
        ASTTreePrinter.printToConsole(program);

        List<String> errors = parser.getErrors();
        if (!errors.isEmpty()) {
            System.out.println("Parser Errors:");
            for (String error : errors) {
                System.out.println(error);
            }
        }
        System.out.println("--- Test " + testName + " Finished ---");
    }
}
