import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;

import java.util.List;

// todo: create string tokenization
// todo: Integer(42.) fix it!
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
                class Test is !!! !928
                    var x := Integer(42.1.1.234.adb.w22) // Some comment
                    method increment() : Integer is
                        return x.Plus(1)
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
