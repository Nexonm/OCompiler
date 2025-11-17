package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an if-then-else statement.
 * Grammar: IfStatement → if Expression then Body [ else Body ] end
 */
public class IfStatement extends Statement {
    private Expression condition; // used for syntax, optimized at semantic stage
    private final List<Statement> thenBranch;
    private final List<Statement> elseBranch;

    public IfStatement(Expression condition, List<Statement> thenBranch,
                       List<Statement> elseBranch, Span span) {
        super(span);
        this.condition = condition;
        this.thenBranch = new ArrayList<>(thenBranch);
        this.elseBranch = elseBranch != null ? new ArrayList<>(elseBranch) : null;
    }

    /**
     * @return condition expression
     */
    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    /**
     * @return then branch (true)
     */
    public List<Statement> getThenBranch() {
        return Collections.unmodifiableList(thenBranch);
    }

    /**
     * @return else branch (false)
     */
    public List<Statement> getElseBranch() {
        return elseBranch != null ? Collections.unmodifiableList(elseBranch) : null;
    }

    /**
     * Used to update the body in case of optimizations
     *
     * @param list new body
     */
    public void setThenBranch(List<Statement> list) {
        if (thenBranch == null) {
            return;
        }
        thenBranch.clear();
        thenBranch.addAll(list);
    }

    /**
     * Used to update the body in case of optimizations
     *
     * @param list new body
     */
    public void setElseBranch(List<Statement> list) {
        if (elseBranch == null) {
            return;
        }
        elseBranch.clear();
        elseBranch.addAll(list);
    }

    /**
     * Check if we have full if-else statement or only if.
     *
     * @return true if have, false otherwise
     */
    public boolean hasElseBranch() {
        return elseBranch != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (hasElseBranch()) {
            return String.format("IfStatement(then: %d stmts, else: %d stmts)",
                    thenBranch.size(), elseBranch.size());
        }
        return String.format("IfStatement(then: %d stmts)", thenBranch.size());
    }
}
