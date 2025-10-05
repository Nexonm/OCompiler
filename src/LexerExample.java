import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.Parser;
import parser.ast.ASTNode;
import parser.ast.declarations.Program;

import java.util.List;

/**
 * Enhanced example usage demonstrating span-based token positioning.
 */
public class LexerExample {
    public static void main(String[] args) {
        test();

    }

    private static void test() {
        System.out.println("=== Simple test for lexer ===\n");
        String code = """
                class Calculator is
                    var result : Integer(0)
                    this() is
                    end
                    method add(a : Integer, b : Integer) : Integer is
                        return Integer(42)
                    end
                    method getValue() : Integer
                end
                """;
        // 1. Tokenize
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        List<String> errors = lexer.getErrors();
        TokenPrinter printer = new TokenPrinter(tokens, errors, code);
        printer.printTokens();
        System.out.println("\n");

        // Step 2: Parse (just pass tokens!)
        Parser parser = new Parser(tokens);
        Program ast = parser.parse();

        // Step 3: Check results
        if (parser.hasErrors()) {
            parser.getErrors().forEach(System.err::println);
        } else {
            System.out.println("Success! Parsed " + ast.getClassCount() + " classes");
        }
        for (ASTNode node : ast.getClasses()){
            System.out.println(node.toString());
        }
    }
}
