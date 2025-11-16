import lexer.Lexer;
import lexer.Token;
import lexer.TokenPrinter;
import parser.ASTTreePrinter;
import parser.Parser;
import parser.ast.ASTNode;
import parser.ast.declarations.Program;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

/**
 * Enhanced example usage demonstrating span-based token positioning.
 */
public class LexerExample {

    private final static String FILE_NAME = "test3.o";
    private final static String DIRECTORY = "./src/tests";
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
