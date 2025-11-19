import codegen.JasminCodeGenerator;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.ASTTreePrinter;
import parser.Parser;
import parser.ast.ASTNode;
import parser.ast.declarations.Program;
import semantic.visitors.ConstantFolder;
import semantic.visitors.DeadCodeReturnEliminator;
import semantic.visitors.SymbolTableBuilder;
import semantic.visitors.TypeChecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

/**
 * Enhanced example usage demonstrating span-based token positioning.
 */
public class LexerExample {

    private final static String FILE_NAME = "test2.o";
    private final static String DIRECTORY = "./src/tests";
    private final static String OUTPUT_DIR = "./src/outcode/src";  // ← ADD THIS
    public static void main(String[] args) {
        test();

    }

    private static void test() {
        System.out.println("======= Lexer Stage =======");
        String code = readFile();
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
        System.out.println("======= Parsing stage =======");

        // Step 2: Parse (just pass tokens!)
        Parser parser = new Parser(tokens);
        Program ast = parser.parse();

        // Step 3: Check results
        ASTTreePrinter astPrinter = new ASTTreePrinter();
        if (parser.hasErrors()){
            System.out.println(astPrinter.printErrors(parser));
        }
        System.out.println(astPrinter.print(ast));
        if (parser.hasErrors()){
            System.err.println("Cannot start Semantic stage because of errors!");
            return;
        }

        System.out.println("======= Semantic stage =======");
        // Step 4: Semantic Analysis
        // Step 4.1: Build basic Symbol Table
        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
        symbolTableBuilder.analyze(ast);
        System.out.println("=== Symbol Table build!");

        // Step 4.2: Type checks
        TypeChecker typeChecker = new TypeChecker(symbolTableBuilder.getGlobalScope());
        typeChecker.check(ast);
        System.out.println("=== Type check: Passed");

        // Step 4.3: Dead code elimination (after return only)
        DeadCodeReturnEliminator deadCodeElim = new DeadCodeReturnEliminator();
        deadCodeElim.optimize(ast);
        System.out.println("=== Return dead code optimization: Passed");
        System.out.println("Statements removed: " + deadCodeElim.getStatementsRemoved());
        System.out.println(astPrinter.print(ast));

        // Step 4.4: Constant Folding
        System.out.println("=== Constant Folding ===");
        int totalFolded = 0;
        int iteration = 0;
        while (true) {
            iteration++;
            ConstantFolder folder = new ConstantFolder();
            boolean changed = folder.optimize(ast);
            totalFolded += folder.getExpressionsFolded();

            if (!changed || folder.getExpressionsFolded() == 0) {
                break;
            }

            if (iteration > 10) {
                System.out.println("Warning: Constant folding exceeded 10 iterations");
                break;
            }
        }
        System.out.println("Constant folding complete: " + totalFolded +
                " expression(s) folded in " + iteration + " pass(es)");



        System.out.println("\n======= Code Generation stage =======");

        // Step 5: Generate Jasmin code
        JasminCodeGenerator codegen = new JasminCodeGenerator(OUTPUT_DIR);

        try {
            codegen.generate(ast);
            System.out.println("=== Code generation: SUCCESS ===");
            System.out.println("Jasmin files written to: " + OUTPUT_DIR);
            System.out.println("\nNext steps:");
            System.out.println("  1. Assemble with Jasmin: java -jar jasmin.jar " + OUTPUT_DIR + "/*.j");
            System.out.println("  2. Run with JVM: java -cp " + OUTPUT_DIR + " YourClassName");
        } catch (Exception e) {
            System.err.println("=== Code generation: FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("\n======= COMPILATION COMPLETE! =======");

    }

    private static String readFile() {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(DIRECTORY + "/" + FILE_NAME))) {
            for(String line; (line = br.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
        }catch (Exception e) {
            System.out.println("Cannot open file: " + FILE_NAME);
            e.printStackTrace();
        }
        return sb.toString();
    }
}
