package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.VariableDecl;
import parser.ast.expressions.Expression;

/**
 * Represents an assignment statement.
 * Grammar: Assignment → Identifier := Expression
 */
public class Assignment extends Statement {
    private final String targetName;
    private final Expression value;

    private VariableDecl resolvedTarget = null;

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

    public VariableDecl getResolvedTarget() {
        return resolvedTarget;
    }

    public void setResolvedTarget(VariableDecl resolvedTarget) {
        this.resolvedTarget = resolvedTarget;
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

