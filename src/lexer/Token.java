package lexer;

/**
 * Represents a lexical token with enhanced span-based positioning.
 * Each token has a type, lexeme (actual text), and precise source location.
 */
public record Token(TokenType type, String lexeme, Span span) {

    /**
     * Creates a new token with validation.
     *
     * @param type   The token type
     * @param lexeme The actual text from the source code
     * @param span   The precise location in the source code
     */
    public Token {
        if (type == null) {
            throw new IllegalArgumentException("Error creating token: token type cannot be null");
        }
        if (lexeme == null) {
            throw new IllegalArgumentException("Error creating token: token lexeme cannot be null");
        }
        if (span == null) {
            throw new IllegalArgumentException("Error creating token: token span cannot be null");
        }
    }


    /**
     * Gets the line number of this token.
     *
     * @return The line number (0-based internally)
     */
    public int line() {
        return span.line();
    }

    /**
     * Gets the starting column of this token.
     *
     * @return The starting column (0-based internally)
     */
    public int startColumn() {
        return span.start();
    }

    /**
     * Gets the ending column of this token.
     *
     * @return The ending column (1-based, exclusive)
     */
    public int endColumn() {
        return span.end();
    }

    /**
     * Gets the length of this token in characters.
     *
     * @return The token length
     */
    public int length() {
        return span.length();
    }

    /**
     * Checks if this token represents a keyword.
     *
     * @return true if this token is a keyword, false otherwise
     */
    public boolean isKeyword() {
        return type.hasFixedRepresentation() &&
                !TokenType.getOperators().contains(type);
    }

    /**
     * Checks if this token represents an operator or delimiter.
     *
     * @return true if this token is an operator, false otherwise
     */
    public boolean isOperator() {
        return TokenType.getOperators().contains(type);
    }

    /**
     * Checks if this token represents a literal value.
     *
     * @return true if this token is a literal, false otherwise
     */
    public boolean isLiteral() {
        return type == TokenType.INTEGER_LITERAL ||
                type == TokenType.REAL_LITERAL ||
                type == TokenType.TRUE ||
                type == TokenType.FALSE;
    }

    /**
     * Checks if token is an error.
     *
     * @return true if token is error, false otherwise
     */
    public boolean isError() {
        return type == TokenType.ERROR;
    }

    /**
     * Returns a formatted error message string for this token.
     *
     * @return Formatted string suitable for error reporting
     */
    public String toErrorString() {
        return String.format("'%s' at %s", lexeme, span.toErrorString());
    }

    @Override
    public String toString() {
        return String.format("lexer.Token{type=%s, lexeme='%s', span=%s}",
                type, lexeme, span);
    }
}
