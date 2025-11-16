package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;

/**
 * Represents a real (floating-point) literal in the source code.
 * <p>
 * Example: 3.14, 0.5, 2.0
 */
public class RealLiteral extends Expression {
    private final double value;

    /**
     * Creates a real literal node.
     *
     * @param value The floating-point value
     * @param span  Position in source code
     */
    public RealLiteral(double value, Span span) {
        super(span);
        this.value = value;
    }

    /**
     * @return The literal value
     */
    public double getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("RealLiteral(%f)", value);
    }
}

