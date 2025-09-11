import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;

import java.util.List;

// todo: create string tokenization
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
                class Test is !!!!
                    var x := Integer(42) // Some comment
                    method increment() : Integer is
                        return x.Plus(1)
                    end -!- -
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
