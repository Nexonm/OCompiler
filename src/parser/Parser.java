package parser;

import lexer.Token;
import lexer.TokenType;
import lexer.Span;
import parser.ast.declarations.ClassDecl;
import parser.ast.declarations.MemberDecl;
import parser.ast.declarations.Program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recursive descent parser for O language.
 * <p>
 * This parser implements a top-down parsing strategy where each grammar rule
 * corresponds to one parsing method. It builds an Abstract Syntax Tree (AST)
 * from a stream of tokens produced by the lexer.
 *
 * Grammar:
 *   Program → { ClassDeclaration }
 *   ClassDeclaration → class Identifier [extends Identifier] is { MemberDeclaration } end
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors = new ArrayList<>();

    /**
     * Creates a new parser with the given token stream.
     * @param tokens List of tokens from the lexer (must include EOF token)
     */
    public Parser(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be null or empty");
        }
        this.tokens = tokens;
    }


    /**
     * Parses the entire token stream and returns the AST root.
     * @return Program node representing the entire source file
     */
    public Program parse() {
        try {
            return parseProgram();
        } catch (Exception e) {
            error("Unexpected error during parsing: " + e.getMessage());
            return new Program(new ArrayList<>()); // Return empty program
        }
    }

    /**
     * Gets the list of parse errors encountered.
     * @return Unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Checks if any errors were encountered during parsing.
     * @return true if errors present, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ========== TOKEN NAVIGATION HELPERS ==========

    /**
     * Returns the current token without consuming it.
     * @return Current token (never null, returns EOF at end)
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the previously consumed token.
     * @return Previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Checks if we've reached the end of the token stream.
     * @return true if current token is EOF, false otherwise
     */
    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    /**
     * Consumes and returns the current token, advancing to the next.
     * @return The consumed token
     */
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    /**
     * Checks if current token is of the given type without consuming it.
     * @param type The token type to check
     * @return true if current token matches, false otherwise
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    /**
     * If current token matches any of the given types, consumes it.
     * @param types Token types to match against
     * @return true if matched and consumed, false otherwise
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes the current token if it matches the expected type.
     * If not, records an error but continues parsing.
     *
     * @param type Expected token type
     * @param message Error message if token doesn't match
     * @return The consumed token (or current token if error)
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        error(message + " (found '" + peek().lexeme() + "')");
        return peek(); // Return current token as fallback
    }

    /**
     * Records a parse error with location information.
     * @param message Error description
     */
    private void error(String message) {
        Token token = peek();
        String errorMsg = String.format("Parse error at %s: %s",
                token.span().toErrorString(),
                message);
        errors.add(errorMsg);
    }

    // ========== PARSING METHODS ==========

    /**
     * Parses the entire program.
     *
     * Grammar: Program → { ClassDeclaration }
     *
     * @return Program node containing all class declarations
     */
    private Program parseProgram() {
        List<ClassDecl> classes = new ArrayList<>();
        while (!isAtEnd()) {
            if (check(TokenType.CLASS)) {
                classes.add(parseClassDeclaration());
            } else {
                // Found something other than 'class' at top level
                error("Expected 'class' declaration at top level");
                advance(); // Skip the invalid token and continue
            }
        }
        return new Program(classes);
    }

    /**
     * Parses a class declaration.
     *
     * Grammar: ClassDeclaration → class Identifier [extends Identifier]
     *                            is { MemberDeclaration } end
     *
     * Phase 1: Parses only empty classes (no members yet)
     *
     * @return ClassDecl node
     */
    private ClassDecl parseClassDeclaration() {
        Token classToken = consume(TokenType.CLASS, "Expected 'class'");
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected class name");
        String className = nameToken.lexeme();
        // Optional: extends BaseClass
        String baseClassName = null;
        if (match(TokenType.EXTENDS)) {
            Token baseToken = consume(TokenType.IDENTIFIER, "Expected base class name");
            baseClassName = baseToken.lexeme();
        }
        consume(TokenType.IS, "Expected 'is'");
        // Phase 1: Skip member parsing for now
        // TODO Phase 2: Parse members (variables, methods, constructors)
        List<MemberDecl> members = new ArrayList<>();
        // For now, just skip to 'end'
        while (!check(TokenType.END) && !isAtEnd()) {
            error("Member declarations not yet implemented (Phase 2)");
            advance(); // Skip unknown tokens
        }
        Token endToken = consume(TokenType.END, "Expected 'end'");
        // Merge spans from 'class' keyword to 'end' keyword
        Span span = classToken.span().merge(endToken.span());
        return new ClassDecl(className, baseClassName, members, span);
    }
}
