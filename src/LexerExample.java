import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;

import java.util.List;

/**
 * Enhanced example usage demonstrating span-based token positioning.
 */
public class LexerExample {
    public static void main(String[] args) {
        test();

    }

    private static void test() {
        System.out.println("=== Testing lexer.Span-Based Error Reporting ===\n");
        String code = """
                class Test is !!!! ? - + 
                    var x := Integer(42)  // Error: should use := 
                    method increment() : Integer is
                        return x.Plus(1) /
                    end
                end
                """;
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        List<String> errors = lexer.getErrors();
        TokenPrinter printer = new TokenPrinter(tokens, errors, code);
        printer.printTokens();
        System.out.println("\n");
    }
}
