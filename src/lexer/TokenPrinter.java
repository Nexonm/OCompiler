package lexer;

import java.util.List;

/**
 * Prints tokens as a propper table.
 */
public class TokenPrinter {

    private final List<Token> tokens;
    private final List<String> errors;
    private final String source;

    public TokenPrinter(List<Token> tokens, List<String> errors, String source) {
        this.tokens = tokens;
        this.errors = errors;
        this.source = source;
    }

    /**
     * Pretty-prints tokens for debugging and testing with enhanced span information.
     */
    public void printTokens() {
        System.out.println("Tokenization Results:");
        System.out.println("=".repeat(60));
        System.out.printf("%-18s | %-12s | %-15s| Additional\n", "Token Type", "Lexeme", "Span");
        System.out.println("=".repeat(60));
        for (Token token : tokens) {
            if (token.type() != TokenType.EOF) {
                String typeInfo = String.format("%-18s", token.type());
                String lexemeInfo = String.format("%-12s", "'" + token.lexeme() + "'");
                String spanInfo = String.format("%-15s", token.span());
                System.out.printf("%s | %s | %s", typeInfo, lexemeInfo, spanInfo);
                if (token.isKeyword()) {
                    System.out.print(" [KEYWORD]");
                } else if (token.isOperator()) {
                    System.out.print(" [OPERATOR]");
                } else if (token.isLiteral()) {
                    System.out.print(" [LITERAL]");
                }else if (token.isError()){
                    System.out.print(" [_ERROR_]");
                }
                System.out.println();
            }
        }
        System.out.println("=".repeat(60));
        System.out.printf("Total tokens: %d%n", tokens.size() - 1); // Exclude EOF
        if (!errors.isEmpty()) {
            System.err.printf("\nErrors: %d\n", errors.size());
            int i = 0;
            for (Token token : tokens) {
                if (token.type() == TokenType.ERROR) {
                    System.err.println("\nError found:");
                    System.err.println(errors.get(i++));
                    highlightSpan(token.span());
                }
            }
        }
    }

    /**
     * Highlights a specific span in the source code for error reporting.
     */
    public void highlightSpan(Span span) {
        String[] lines = source.split("\n");
        if (span.line() < lines.length) {
            String sourceLine = lines[span.line()];
            System.err.println(sourceLine);
            String highlight = " ".repeat(Math.max(0, span.start())) +
                    "^".repeat(Math.max(0, span.end() - span.start()));
            System.err.println(highlight);
        }
    }

}
