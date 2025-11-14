package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;

public class UnknownExpression extends Expression {
    public UnknownExpression(Span span) {
        super(span);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }
}
