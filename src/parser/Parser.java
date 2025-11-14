package parser;

import lexer.Token;
import lexer.TokenType;
import lexer.Span;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

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
        Token classToken = consume(TokenType.CLASS, "Expected 'class' keyword");
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected class name");
        String className = nameToken.lexeme();
        // Optional: extends BaseClass
        String baseClassName = null;
        if (match(TokenType.EXTENDS)) {
            Token baseToken = consume(TokenType.IDENTIFIER, "Expected base class name");
            baseClassName = baseToken.lexeme();
        }
        consume(TokenType.IS, "Expected 'is' after class declaration");
        // Parse members
        List<MemberDecl> members = new ArrayList<>();
        while (!check(TokenType.END) && !isAtEnd()) {
            // Check what kind of member this is
            if (check(TokenType.VAR)) {
                members.add(parseVariableDeclaration());
            } else if (check(TokenType.METHOD)) {
                members.add(parseMethodDeclaration());
            } else if (check(TokenType.THIS)) {
                members.add(parseConstructorDeclaration());
            } else {
                error("Expected member declaration (var, method, or this), got \"" + peek().lexeme() + "\" instead");
                advance(); // Skip invalid token
            }
        }
        Token endToken = consume(TokenType.END, "Expected 'end' to close the class " + className);
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
     * Parses a method declaration (NEW in Phase 3).
     * Grammar: MethodDeclaration → method Identifier [ Parameters ] [ : Identifier ] [ MethodBody ]
     *
     * Examples:
     * - method foo()
     * - method add(a : Integer, b : Integer) : Integer
     * - method getValue() : Integer is return count end
     */
    private MethodDecl parseMethodDeclaration() {
        Token methodToken = consume(TokenType.METHOD, "Expected 'method'");
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected method name");
        String methodName = nameToken.lexeme();
        // Parse parameters (optional)
        List<Parameter> parameters = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            parameters = parseParameters();
            consume(TokenType.RPAREN, "Expected ',' between or ')' after parameters");
        }
        // Parse return type (optional)
        String returnTypeName = null;
        if (match(TokenType.COLON)) {
            Token typeToken = consume(TokenType.IDENTIFIER, "Expected return type name");
            returnTypeName = typeToken.lexeme();
        }
        // Parse body (optional - forward declaration if missing)
        List<Statement> body = null;
        Span endSpan = previous().span();
        if (match(TokenType.IS)) {
            body = parseMethodBody();
            Token endToken = consume(TokenType.END, "Expected 'end'");
            endSpan = endToken.span();
        } else if (match(TokenType.ARROW)) {
            Expression value = parseExpression();
            body = new ArrayList<>();
            body.add(new ReturnStatement(value, value.getSpan()));
            endSpan = value.getSpan();
        }

        Span span = methodToken.span().merge(endSpan);
        return new MethodDecl(methodName, parameters, returnTypeName, body, span);
    }

    /**
     * Parses a constructor declaration (NEW in Phase 3).
     * Grammar: ConstructorDeclaration → this [ Parameters ] is Body end
     *
     * Example:
     * this(initialCount : Integer) is
     *     count := initialCount
     * end
     */
    private ConstructorDecl parseConstructorDeclaration() {
        Token thisToken = consume(TokenType.THIS, "Expected 'this'");

        // Parse parameters (optional)
        List<Parameter> parameters = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            parameters = parseParameters();
            consume(TokenType.RPAREN, "Expected ',' between or ')' after parameters");
        }

        consume(TokenType.IS, "Expected 'is'");
        List<Statement> body = parseMethodBody();
        Token endToken = consume(TokenType.END, "Expected 'end'");

        Span span = thisToken.span().merge(endToken.span());
        return new ConstructorDecl(parameters, body, span);
    }

    /**
     * Parses method/constructor parameters (NEW in Phase 3).
     * Grammar: Parameters → [ Parameter { , Parameter } ]
     *          Parameter → Identifier : Identifier
     *
     * Note: Call this AFTER consuming the opening '('
     */
    private List<Parameter> parseParameters() {
        List<Parameter> parameters = new ArrayList<>();
        // Empty parameter list?
        if (check(TokenType.RPAREN)) {
            return parameters;
        }
        // Parse first parameter
        parameters.add(parseParameter());
        // Parse remaining parameters
        while (match(TokenType.COMMA)) {
            parameters.add(parseParameter());
        }
        return parameters;
    }

    /**
     * Parses a single parameter
     * Grammar: Parameter → Identifier : Identifier
     *
     * Example: count : Integer
     */
    private Parameter parseParameter() {
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected parameter name");
        consume(TokenType.COLON, "Expected ':' after parameter name");
        Token typeToken = consume(TokenType.IDENTIFIER, "Expected parameter type");

        Span span = nameToken.span().merge(typeToken.span());
        return new Parameter(nameToken.lexeme(), typeToken.lexeme(), span);
    }

    /**
     * Parses a method or constructor body
     * Grammar: Body → { Statement }
     *
     * Note: Call this AFTER consuming 'is'
     */
    private List<Statement> parseMethodBody() {
        List<Statement> statements = new ArrayList<>();

        while (!check(TokenType.END) && !isAtEnd()) {
            statements.add(parseStatement());
        }

        return statements;
    }

    /**
     * Parses a statement
     * Grammar: Statement → ReturnStatement | Assignment | IfStatement | WhileLoop
     */
    private Statement parseStatement() {
        if (check(TokenType.VAR)) {
            VariableDecl decl = parseVariableDeclaration();
            return new VariableDeclStatement(decl, decl.getSpan());
        }
        if (check(TokenType.RETURN)) {
            return parseReturnStatement();
        }
        if (check(TokenType.IF)) {
            return parseIfStatement();
        }
        if (check(TokenType.WHILE)) {
            return parseWhileLoop();
        }
        if (check(TokenType.IDENTIFIER)) {
            int saved = current;
            Token id = advance();
            if (check(TokenType.ASSIGNMENT)) {
                current = saved;
                return parseAssignment();
            }
            current = saved;
        }
        error("Expected statement (var, return, if, while, or assignment), found (" + peek().lexeme() + ")");
        advance();
        return new UnknownStatement(null, previous().span());
    }

    /**
     * Parses an assignment statement.
     * Grammar: Assignment → Identifier := Expression
     * Example: count := Integer(42)
     *
     * @return Assignment statement node
     */
    private Assignment parseAssignment() {
        Token targetToken = consume(TokenType.IDENTIFIER, "Expected variable name");
        String targetName = targetToken.lexeme();
        consume(TokenType.ASSIGNMENT, "Expected ':='");
        Expression value = parseExpression();
        Span span = targetToken.span().merge(value.getSpan());
        return new Assignment(targetName, value, span);
    }

    /**
     * Parses an if-then-else statement.
     * Grammar: IfStatement → if Expression then Body [ else Body ] end
     * Example: if x.Greater(y) then return x else return y end
     *
     * The else branch is optional. Both then and else branches can contain
     * multiple statements.
     *
     * @return IfStatement node with condition, then-branch, and optional else-branch
     */
    private IfStatement parseIfStatement() {
        Token ifToken = consume(TokenType.IF, "Expected 'if'");
        Expression condition = parseExpression();
        consume(TokenType.THEN, "Expected 'then'");
        List<Statement> thenBranch = new ArrayList<>();
        while (!check(TokenType.ELSE) && !check(TokenType.END) && !isAtEnd()) {
            thenBranch.add(parseStatement());
        }
        List<Statement> elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = new ArrayList<>();
            while (!check(TokenType.END) && !isAtEnd()) {
                elseBranch.add(parseStatement());
            }
        }
        Token endToken = consume(TokenType.END, "Expected 'end'");
        Span span = ifToken.span().merge(endToken.span());
        return new IfStatement(condition, thenBranch, elseBranch, span);
    }

    /**
     * Parses a while loop statement.
     * Grammar: WhileLoop → while Expression loop Body end
     * Example: while count.Greater(Integer(0)) loop count := count.Minus(Integer(1)) end
     *
     * The loop body can contain multiple statements.
     *
     * @return WhileLoop node with condition and body statements
     */
    private WhileLoop parseWhileLoop() {
        Token whileToken = consume(TokenType.WHILE, "Expected 'while'");
        Expression condition = parseExpression();
        consume(TokenType.LOOP, "Expected 'loop'");
        List<Statement> body = new ArrayList<>();
        while (!check(TokenType.END) && !isAtEnd()) {
            body.add(parseStatement());
        }
        Token endToken = consume(TokenType.END, "Expected 'end'");
        Span span = whileToken.span().merge(endToken.span());
        return new WhileLoop(condition, body, span);
    }

    /**
     * Parses a return statement
     * Grammar: ReturnStatement → return [ Expression ]
     *
     * Examples:
     * - return           (void return)
     * - return x         (return value)
     * - return x.Plus(y) (return expression)
     */
    private ReturnStatement parseReturnStatement() {
        Token returnToken = consume(TokenType.RETURN, "Expected 'return'");
        // Check if there's a return value
        Expression value = null;
        if (!check(TokenType.END) && !check(TokenType.RETURN) && !isAtEnd()) {
            // Try to parse expression
            // For now, simple heuristic: if next token could start expression
            if (check(TokenType.IDENTIFIER) || check(TokenType.INTEGER_LITERAL) ||
                    check(TokenType.REAL_LITERAL) || check(TokenType.TRUE) ||
                    check(TokenType.FALSE) || check(TokenType.THIS) ||
                    check(TokenType.LPAREN)) {
                value = parseExpression();
            }
        }
        Span span = value != null ?
                returnToken.span().merge(value.getSpan()) :
                returnToken.span();
        return new ReturnStatement(value, span);
    }

    private Expression parseExpression() {
        Expression expr = parsePrimary();
        while (match(TokenType.DOT)) {
            Token nameToken = consume(TokenType.IDENTIFIER, "Expected member or method name");
            String name = nameToken.lexeme();
            if (match(TokenType.LPAREN)) {
                List<Expression> args = parseArguments();
                Token rparen = consume(TokenType.RPAREN, "Expected ')'");
                Span span = expr.getSpan().merge(rparen.span());
                expr = new MethodCall(expr, name, args, span);
            } else {
                Span span = expr.getSpan().merge(nameToken.span());
                expr = new MemberAccess(expr, name, span);
            }
        }
        return expr;
    }


    /**
     * Parses a primary expression
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
        return new UnknownExpression(peek().span());
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
