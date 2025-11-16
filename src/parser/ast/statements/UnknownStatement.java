package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

public class UnknownStatement extends Statement {
    private final Expression value; // null for void return

    /**
     * Creates a return statement node.
     *
     * @param value The return value expression (null for void)
     * @param span  Position in source code
     */
    public UnknownStatement(Expression value, Span span) {
        super(span);
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("ERROR: Unknown Statement", value);
    }
}