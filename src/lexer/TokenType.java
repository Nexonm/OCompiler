package lexer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * lexer.Token type representation through enum with defined keywords as representations.
 */
public enum TokenType {
    // Class and inheritance keywords
    CLASS("class"),
    EXTENDS("extends"),
    IS("is"),
    END("end"),

    // Variable and method declaration keywords
    VAR("var"),
    METHOD("method"),
    THIS("this"),

    // Control flow keywords
    IF("if"),
    THEN("then"),
    ELSE("else"),
    WHILE("while"),
    LOOP("loop"),
    RETURN("return"),

    // Boolean keywords
    TRUE("true"),
    FALSE("false"),

    // Multi-character operators
    ASSIGNMENT(":="),
    ARROW("=>"),

    // Single-character tokens
    COLON(":"),
    DOT("."),
    LPAREN("("),
    RPAREN(")"),
    LBRACKET("["),
    RBRACKET("]"),
    LBRACE("{"),
    RBRACE("}"),
    COMMA(","),

    // Variable tokens (no fixed string representation)
    // User defined representation called lexeme
    IDENTIFIER(null),
    INTEGER_LITERAL(null),
    REAL_LITERAL(null),
    STRING_LITERAL(null),

    // Special tokens
    WHITESPACE(null),
    NEWLINE(null),
    EOF(null),
    ERROR(null);

    private final String representation;

    /**
     * @param representation The string representation of the token, or null for variable tokens
     */
    TokenType(String representation) {
        this.representation = representation;
    }

    /**
     * @return The string representation, or null if this token has variable content
     */
    public String getRepresentation() {
        return representation;
    }

    /**
     * Checks if this token type has a fixed string representation, or it is user-defined.
     * @return true if the token has a fixed representation, false otherwise
     */
    public boolean hasFixedRepresentation() {
        return representation != null;
    }

    /**
     * Search through all token types to find keyword matches by string.
     * @param text The text to look up
     * @return The corresponding lexer.TokenType, or IDENTIFIER if not found
     */
    public static TokenType fromString(String text) {
        for (TokenType tokenType : TokenType.values()) {
            // No ignore case accepted as keywords should be lowercase only
            if (text.equals(tokenType.getRepresentation())) {
                return tokenType;
            }
        }
        return IDENTIFIER; // Default for unrecognized text
    }

    /**
     * Gets all keywords (tokens with fixed representations).
     * Omits tokens with representation=null
     * @return Set of all keyword token types
     */
    public static Set<TokenType> getKeywords() {
        return Arrays.stream(TokenType.values())
                .filter(TokenType::hasFixedRepresentation)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all operator tokens.
     * @return Set of operator token types
     */
    public static Set<TokenType> getOperators() {
        return Set.of(ASSIGNMENT, ARROW, COLON, DOT, LPAREN, RPAREN,
                LBRACKET, RBRACKET, LBRACE, RBRACE, COMMA);
    }
}