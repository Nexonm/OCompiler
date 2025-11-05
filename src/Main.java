import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.ASTTreePrinter;
import parser.Parser;
import parser.ast.declarations.Program;
import semantic.SymbolTablePrinter;
import semantic.visitor.DeclarationChecker;
import semantic.visitor.SymbolTableBuilder;
import semantic.visitor.TypeConsistencyChecker;

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
                class Calculator is
                       var result : Integer(0)
                
                       this(initialValue : Integer) is
                           result := initialValue
                       end
                
                       method add(value : Integer) is
                           result := result.Plus(value)
                       end
                
                       method getResult() : Integer is
                           result
                       end
                   end
                
                   class Main is
                       var calc : Calculator(Integer(5))
                
                       method run() is
                           calc.add(Integer(3))
                           calc.add(Integer(2))
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

        // Step 6: check declaration before usage
        DeclarationChecker declChecker = new DeclarationChecker(builder.getSymbolTable());
        boolean declCheckPassed = declChecker.check(ast);
        System.out.println("Declaration check: " + (declCheckPassed? "passed!" : "❌!"));
        declChecker.printErrors();

        // Step 7: check type consistency
        TypeConsistencyChecker typeChecker = new TypeConsistencyChecker(builder.getSymbolTable());
        boolean typeCheckPassed = typeChecker.check(ast);
        System.out.println("Type Consistency check: " + (typeCheckPassed? "passed!" : "❌!"));
            typeChecker.printErrors();
    }
}
