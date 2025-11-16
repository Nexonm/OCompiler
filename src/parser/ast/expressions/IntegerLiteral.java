package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;

/**
 * Represents an integer literal in the source code.
 * <p>
 * Example: 42, 0, -5
 * <p>
 * Note: Negative numbers are initially parsed as positive literals
 * and the UnaryMinus operator is applied during semantic analysis.
 */
public class IntegerLiteral extends Expression {
    private final int value;

    /**
     * Creates an integer literal node.
     *
     * @param value The integer value
     * @param span  Position in source code
     */
    public IntegerLiteral(int value, Span span) {
        super(span);
        this.value = value;
    }

    /**
     * @return The literal value
     */
    public int getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("IntegerLiteral(%d)", value);
    }
}

