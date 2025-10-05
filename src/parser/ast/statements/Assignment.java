package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

/**
 * Represents an assignment statement.
 * Grammar: Assignment → Identifier := Expression
 */
public class Assignment extends Statement {
    private final String targetName;
    private final Expression value;

    public Assignment(String targetName, Expression value, Span span) {
        super(span);
        this.targetName = targetName;
        this.value = value;
    }

    /**
     * @return target name
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * @return value
     */
    public Expression getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("Assignment(%s := %s)", targetName, value);
    }
}

