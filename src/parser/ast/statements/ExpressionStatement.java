package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

/**
 * Represents an expression used as a statement.
 * <p>
 * Example: myObject.someMethod()
 */
public class ExpressionStatement extends Statement {
    private final Expression expression;

    public ExpressionStatement(Expression expression, Span span) {
        super(span);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitExpressionStatement(this);
    }

    @Override
    public String toString() {
        return "ExpressionStatement(" + expression + ")";
    }
}
