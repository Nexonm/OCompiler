import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.ASTTreePrinter;
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
                class Counter is
                       var count : Integer(0)
                
                       method increment() is
                           count := count.add(Integer(1))
                       end
                
                       method getValue() : Integer is
                           if count.graterThan(Integer(0)) then
                               return count
                           else
                               return Integer(0)
                           end
                       end
                
                       method reset() is
                           while count.graterThan(Integer(5)) loop
                               count := count.minus(Integer(1))
                           end
                       end
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
        ASTTreePrinter astPrinter = new ASTTreePrinter();
        System.out.println(astPrinter.print(ast));
    }
}
