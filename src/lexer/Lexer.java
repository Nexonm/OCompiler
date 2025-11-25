package lexer;

import java.nio.file.Files;
import java.util.*;
import java.io.*;

/**
 * Enhanced lexical analyzer for the O programming language with span-based positioning.
 * <p>
 * This lexer tokenizes O language source code into a stream of tokens with precise
 * source location information using lexer.Span objects for better error reporting and
 * IDE integration capabilities.
 */
public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // Current scanning position
    private int current = 0;
    private int line = 0;
    private int column = 0;

    // Error collection
    private final List<String> errors = new ArrayList<>();

    /**
     * Creates a new lexer for the given source code.
     *
     * @param source The O language source code to tokenize
     */
    public Lexer(String source) {
        this.source = source != null ? source : "";
    }

    /**
     * Creates a lexer from a file.
     *
     * @param file The file containing O language source code
     * @throws IOException if the file cannot be read
     */
    public static Lexer fromFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        return new Lexer(content);
    }

    /**
     * Tokenizes the entire source code.
     *
     * @return List of tokens including EOF token at the end
     */
    public List<Token> tokenize() {
        tokens.clear();
        errors.clear();
        current = 0;
        line = 0;
        column = 0;
        while (!isAtEnd()) {
            tokenizeNext();
        }
        // Always add EOF token to address the end of span
        Span eofSpan = Span.empty(line, column);
        tokens.add(new Token(TokenType.EOF, "", eofSpan));
        return new ArrayList<>(tokens); // Return defensive copy
    }


    /**
     * Tokenizes the next token from the current position.
     */
    private void tokenizeNext() {
        int startLine = line;
        int startColumn = column;
        char c = next();

        switch (c) {
            // Single-character tokens
            case '(' -> addSingleCharToken(TokenType.LPAREN, startLine, startColumn);
            case ')' -> addSingleCharToken(TokenType.RPAREN, startLine, startColumn);
            case '[' -> addSingleCharToken(TokenType.LBRACKET, startLine, startColumn);
            case ']' -> addSingleCharToken(TokenType.RBRACKET, startLine, startColumn);
            case '{' -> addSingleCharToken(TokenType.LBRACE, startLine, startColumn);
            case '}' -> addSingleCharToken(TokenType.RBRACE, startLine, startColumn);
            case ',' -> addSingleCharToken(TokenType.COMMA, startLine, startColumn);
            case '.' -> addSingleCharToken(TokenType.DOT, startLine, startColumn);

            case '-' -> {
                if (isDigit(peek())) {
                    tokenizeNumber(startLine, startColumn);
                } else {
                    reportError("Unexpected character '-'", startLine, startColumn);
                }
            }

            // Multi-character tokens starting with ':'
            case ':' -> {
                if (match('=')) {
                    addMultiCharToken(TokenType.ASSIGNMENT, ":=", startLine, startColumn);
                } else {
                    addSingleCharToken(TokenType.COLON, startLine, startColumn);
                }
            }

            // Multi-character tokens starting with '='
            case '=' -> {
                if (match('>')) {
                    addMultiCharToken(TokenType.ARROW, "=>", startLine, startColumn);
                } else {
                    reportError("Unexpected character '='. Did you mean '=>'?", startLine, startColumn);
                }
            }

            // Whitespace handling
            case ' ', '\r', '\t' -> {
                // Skip whitespace, but track column position
            }

            case '\n' -> {
                line++;
                column = 0; // Will be incremented at the start of loop
            }

            // Comments
            case '/' -> {
                if (match('/')) {
                    // Line comment - skip until end of line
                    while (!isAtEnd() && peek() != '\n') {
                        next();
                    }
                } else if (match('*')) {
                    // Block comment - skip until */
                    scanBlockComment(startLine, startColumn);
                } else {
                    reportError("Unexpected character '/'", startLine, startColumn);
                }
            }

            // String literals
            case '"' -> tokenizeString(startLine, startColumn);

            default -> {
                if (isDigit(c)) {
                    tokenizeNumber(startLine, startColumn);
                } else if (isIdentifierStart(c)) {
                    tokenizeIdentifier(startLine, startColumn);
                } else {
                    reportError("Unexpected character '" + c + "'", startLine, startColumn);
                }
            }
        }
    }

    /**
     * Tokenizes a numeric literal (integer or real).
     * Reads everything until RPAREN, then validates if it's a valid integer or double.
     *
     * @param startLine   The starting line of the number token.
     * @param startColumn The starting column of the number token.
     */
    private void tokenizeNumber(int startLine, int startColumn) {
        // Back up to include the first digit we already consumed
        current--;
        column--;
        int start = current;

        boolean hasDot = false;
        while (!isAtEnd()) {
            char c = peek();
            if (isDigit(c)) {
                next();
            } else if (c == '.') {
                if (hasDot) {
                    // Second dot found (e.g. 1.2.3), stop here
                    reportError("Invalid numeric literal", startLine, startColumn, current - start + 1);
                    break;
                }

                if (isDigit(peekNext())) {
                    hasDot = true;
                    next();
                } else {
                    // Dot not followed by digit, likely member access (5.Plus)
                    reportError("Invalid numeric literal", startLine, startColumn, current - start + 1);
                    break;
                }
            } else {
                break;
            }
        }

        String lexeme = source.substring(start, current);
        Span span = Span.singleLine(startLine, startColumn, column);
        // Try to parse as integer first
        try {
            Integer.parseInt(lexeme);
            tokens.add(new Token(TokenType.INTEGER_LITERAL, lexeme, span));
            return;
        } catch (NumberFormatException e) {
            // Not an integer, try double
        }
        // Try to parse as double
        try {
            Double.parseDouble(lexeme);
            tokens.add(new Token(TokenType.REAL_LITERAL, lexeme, span));
            return;
        } catch (NumberFormatException e) {
            // Not a valid number at all
        }
        // Both parsing attempts failed - report error
        reportError("Invalid numeric literal", startLine, startColumn, lexeme.length());
    }


    /**
     * Tokenizes a string literal.
     *
     * @param startLine   The starting line of the string token.
     * @param startColumn The starting column of the string token.
     */
    private void tokenizeString(int startLine, int startColumn) {
        // We already consumed the opening '"'
        int start = current - 1;
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') {
                reportError("Unterminated string.", startLine, startColumn, current - start);
                line++;
                column = 0;
                return;
            }
            next();
        }
        if (isAtEnd()) {
            reportError("Unterminated string.", startLine, startColumn, current - start);
            return;
        }
        // Consume the closing '"'
        next();
        String lexeme = source.substring(start, current);
        Span span = Span.singleLine(startLine, startColumn, column);
        tokens.add(new Token(TokenType.STRING_LITERAL, lexeme, span));
    }


    /**
     * Tokenizes an identifier or keyword.
     * Uses the lexer.TokenType.fromString() method for keyword lookup.
     *
     * @param startLine   current line
     * @param startColumn current start position
     */
    private void tokenizeIdentifier(int startLine, int startColumn) {
        // Back up to include the first character we already consumed
        current--;
        column--;
        int start = current;
        // Consume all identifier characters
        while (!isAtEnd() && isIdentifierContinue(peek())) {
            next();
        }
        String lexeme = source.substring(start, current);
        TokenType type = TokenType.fromString(lexeme);
        Span span = Span.singleLine(startLine, startColumn, column);
        tokens.add(new Token(type, lexeme, span));
    }

    /**
     * Handles block comments.
     *
     * @param startLine   start line of comment
     * @param startColumn start column of comment
     */
    private void scanBlockComment(int startLine, int startColumn) {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                next(); // consume '*'
                next(); // consume '/'
                return;
            }
            if (peek() == '\n') {
                line++;
                column = 0;
            }
            next();
        }
        reportError("Unterminated block comment", startLine, startColumn);
    }

    // Methods for character classification

    /**
     * Checks if current position at end or out of it.
     *
     * @return true if at end or out of it, false otherwise
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Increments column, returns current character and increments current afterwards.
     *
     * @return next character
     */
    private char next() {
        column++;
        return source.charAt(current++);
    }

    /**
     * Called when column < current. Hence, it checks if next char matches one provided.
     *
     * @param expected provided char to match to.
     * @return true if next char is same, else otherwise
     */
    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }

        current++;
        column++;
        return true;
    }

    /**
     * Returns current character without incrementing the position.
     *
     * @return current character
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Returns next character without incrementing the position.
     *
     * @return next character
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Checks if a provided character is a digit.
     *
     * @param c character to be checked
     * @return true if it is a digit, false otherwise
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if a provided character is a letter or _ (underscore).
     *
     * @param c character to be checked
     * @return true if letter or underscore, false otherwise
     */
    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_'; // do we allow "_someNameStart"? \\todo check if we allow it
    }

    /**
     * Checks if provided character is an inner part of identifier. The inner part
     * can be alphanumeric.
     *
     * @param c character to check
     * @return true if character alphanumeric, false otherwise
     */
    private boolean isIdentifierContinue(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    /**
     * Adds a single-character token.
     *
     * @param type        type of token
     * @param startLine   line of token span
     * @param startColumn start and end of token span
     */
    private void addSingleCharToken(TokenType type, int startLine, int startColumn) {
        String lexeme = type.getRepresentation();
        if (lexeme == null) {
            lexeme = String.valueOf(source.charAt(current - 1));
        }
        Span span = Span.single(startLine, startColumn);
        tokens.add(new Token(type, lexeme, span));
    }

    /**
     * Adds a multi-character token (examples: ':=', '=>').
     *
     * @param type        type of token
     * @param lexeme      token representation
     * @param startLine   line of token span
     * @param startColumn start of token span
     */
    private void addMultiCharToken(TokenType type, String lexeme, int startLine, int startColumn) {
        Span span = Span.singleLine(startLine, startColumn, column);
        tokens.add(new Token(type, lexeme, span));
    }

    /**
     * Records a lexical error with span information.
     * Automatically calculates the size of the error by consuming problematic characters.
     *
     * @param message     error message
     * @param startLine   line where the problem found
     * @param startColumn start of problematic sequence
     */
    private void reportError(String message, int startLine, int startColumn) {
        int length = 1;
        while (!isAtEnd() && !isOperator(peek()) && !isEndChar(peek())) {
            next();
            length++;
        }
        reportError(message, startLine, startColumn, length);
    }

    /**
     * Records a lexical error with span information and explicit size.
     *
     * @param message     error message
     * @param startLine   line where the problem found
     * @param startColumn start of problematic sequence
     * @param length      size of the error lexeme
     */
    private void reportError(String message, int startLine, int startColumn, int length) {
        int start = current - length;
        Span errorSpan = Span.singleLine(startLine, startColumn, startColumn + length);
        String error = String.format("Lexical error at %s: %s", errorSpan.toErrorString(), message);
        errors.add(error);
        // Add an error token to continue parsing
        tokens.add(new Token(TokenType.ERROR, source.substring(start, start + length), errorSpan));
    }

    /**
     * Checks if character is an operator.
     *
     * @param c character to check
     * @return true if operator, false otherwise
     */
    private boolean isOperator(char c) {
        TokenType type = TokenType.fromString(Character.toString(c));
        return TokenType.getOperators().contains(type);
    }

    private boolean isEndChar(char c) {
        switch (c) {
            case ' ', '\r', '\t', '\n' -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Gets any lexical errors encountered during tokenization.
     *
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }


}

