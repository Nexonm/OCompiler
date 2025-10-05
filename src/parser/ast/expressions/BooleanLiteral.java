package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;

/**
 * Represents a boolean literal in the source code.
 *
 * Example: true, false
 */
public class BooleanLiteral extends Expression {
    private final boolean value;

    /**
     * Creates a boolean literal node.
     * @param value The boolean value (true or false)
     * @param span Position in source code
     */
    public BooleanLiteral(boolean value, Span span) {
        super(span);
        this.value = value;
    }

    /**
     * @return true or false
     */
    public boolean getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("BooleanLiteral(%b)", value);
    }
}

