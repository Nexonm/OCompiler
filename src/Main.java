import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.ASTTreePrinter;
import parser.Parser;
import parser.ast.declarations.Program;
import semantic.SymbolTablePrinter;
import semantic.symbols.ClassSymbol;
import semantic.symbols.SymbolTable;
import semantic.visitor.SymbolTableBuilder;

import java.util.List;

/**
 * Enhanced example usage demonstrating span-based token positioning.
 */
public class Main {
    public static void main(String[] args) {
        test();

    }

    private static void test() {
        System.out.println("=== Simple test for lexer ===\n");
        String code = """
              class Counter is
                  var count : Integer(0)

                  method increment(b: Integer) is
                      count := count.Plus(b)
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

        if (!errors.isEmpty()){
            System.err.println("Cannot start Parsing stage because of errors!");
            return;
        }
        System.out.println("=== Parsing stage ===");

        // Step 2: Parse (just pass tokens!)
        Parser parser = new Parser(tokens);
        Program ast = parser.parse();

        // Step 3: Check results
        ASTTreePrinter.printToConsole(ast);

        // Step 4: Build Symbol Table
        SymbolTableBuilder builder = new SymbolTableBuilder();
        if (!builder.build(ast)) {
            builder.printErrors();
        }
        System.out.println("=== Symbol table built! ===");

        // Step 5: print table
        SymbolTablePrinter tablePrinter = new SymbolTablePrinter(true);
        tablePrinter.print(builder.getSymbolTable());
    }
}
