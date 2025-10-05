package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;

/**
 * Represents the 'this' keyword in an expression.
 * <p>
 * Used inside methods to refer to the current object.
 *
 * Example: this.member, return this
 */
public class ThisExpr extends Expression {

    /**
     * Creates a 'this' expression node.
     * @param span Position in source code
     */
    public ThisExpr(Span span) {
        super(span);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return "ThisExpression";
    }
}
