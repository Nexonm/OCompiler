package parser;

import lexer.Token;
import lexer.TokenType;
import lexer.Span;
import parser.ast.declarations.*;
import parser.ast.expressions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recursive descent parser for O language.
 * <p>
 * This parser implements a top-down parsing strategy where each grammar rule
 * corresponds to one parsing method. It builds an Abstract Syntax Tree (AST)
 * from a stream of tokens produced by the lexer.
 * <p>
 * Grammar:
 *   Program → { ClassDeclaration }
 *   ClassDeclaration → class Identifier [extends Identifier] is { MemberDeclaration } end
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be null or empty");
        }
        this.tokens = tokens;
    }


    public Program parse() {
        try {
            return parseProgram();
        } catch (Exception e) {
            error("Unexpected error during parsing: " + e.getMessage());
            e.printStackTrace(); // For debugging
            return new Program(new ArrayList<>());
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
        return peek();
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
        // Parse members (NEW in Phase 2!)
        List<MemberDecl> members = new ArrayList<>();
        while (!check(TokenType.END) && !isAtEnd()) {
            // Check what kind of member this is
            if (check(TokenType.VAR)) {
                members.add(parseVariableDeclaration());
            } else if (check(TokenType.METHOD)) {
                error("Method declarations not yet implemented (Phase 3)");
                advance(); // Skip for now
            } else if (check(TokenType.THIS)) {
                error("Constructor declarations not yet implemented (Phase 3)");
                advance(); // Skip for now
            } else {
                error("Expected member declaration (var, method, or this)");
                advance(); // Skip invalid token
            }
        }

        Token endToken = consume(TokenType.END, "Expected 'end'");

        Span span = classToken.span().merge(endToken.span());
        return new ClassDecl(className, baseClassName, members, span);
    }

    /**
     * Parses a variable declaration (NEW in Phase 2).
     * Grammar: VariableDeclaration → var Identifier : Expression
     *
     * Example: var x : Integer(42)
     */
    private VariableDecl parseVariableDeclaration() {
        Token varToken = consume(TokenType.VAR, "Expected 'var'");
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected variable name");
        consume(TokenType.COLON, "Expected ':'");

        Expression initializer = parseExpression();

        Span span = varToken.span().merge(initializer.getSpan());
        return new VariableDecl(nameToken.lexeme(), initializer, span);
    }

    /**
     * Parses an expression (NEW in Phase 2).
     * Grammar: Expression → Primary | ConstructorCall
     *
     * Phase 2 limitation: No method calls or member access yet
     */
    private Expression parseExpression() {
        return parsePrimary();
    }

    /**
     * Parses a primary expression (NEW in Phase 2).
     * Grammar: Primary → IntegerLiteral | RealLiteral | BooleanLiteral
     *                   | this | Identifier | ConstructorCall
     */
    private Expression parsePrimary() {
        if (check(TokenType.INTEGER_LITERAL)) {
            Token token = advance();
            try {
                int value = Integer.parseInt(token.lexeme());
                return new IntegerLiteral(value, token.span());
            } catch (NumberFormatException e) {
                error("Invalid integer literal: " + token.lexeme());
                return new IntegerLiteral(0, token.span());
            }
        }
        if (check(TokenType.REAL_LITERAL)) {
            Token token = advance();
            try {
                double value = Double.parseDouble(token.lexeme());
                return new RealLiteral(value, token.span());
            } catch (NumberFormatException e) {
                error("Invalid real literal: " + token.lexeme());
                return new RealLiteral(0.0, token.span());
            }
        }
        if (match(TokenType.TRUE)) {
            return new BooleanLiteral(true, previous().span());
        }
        if (match(TokenType.FALSE)) {
            return new BooleanLiteral(false, previous().span());
        }
        if (match(TokenType.THIS)) {
            return new ThisExpr(previous().span());
        }
        if (check(TokenType.IDENTIFIER)) {
            Token token = advance();
            String name = token.lexeme();
            if (match(TokenType.LPAREN)) {
                List<Expression> args = parseArguments();
                Token rparen = consume(TokenType.RPAREN, "Expected ')'");
                Span span = token.span().merge(rparen.span());
                return new ConstructorCall(name, args, span);
            }
            return new IdentifierExpr(name, token.span());
        }
        error("Expected expression");
        return new IdentifierExpr("ERROR", peek().span());
    }

    /**
     * Parses a comma-separated list of argument expressions (NEW in Phase 2).
     * Grammar: Arguments → Expression { , Expression }
     *
     * Note: Call this AFTER consuming the opening '('
     */
    private List<Expression> parseArguments() {
        List<Expression> args = new ArrayList<>();
        if (check(TokenType.RPAREN)) {
            return args;
        }
        args.add(parseExpression());
        while (match(TokenType.COMMA)) {
            args.add(parseExpression());
        }
        return args;
    }
}
