package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a while loop.
 * Grammar: WhileLoop → while Expression loop Body end
 */
public class WhileLoop extends Statement {
    private final Expression condition;
    private final List<Statement> body;

    public WhileLoop(Expression condition, List<Statement> body, Span span) {
        super(span);
        this.condition = condition;
        this.body = new ArrayList<>(body);
    }

    /**
     * @return condition expression
     */
    public Expression getCondition() {
        return condition;
    }

    /**
     * @return loop body consisting of {@link Statement}s
     */
    public List<Statement> getBody() {
        return Collections.unmodifiableList(body);
    }

    /**
     * @return number of statements
     */
    public int getBodySize() {
        return body.size();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("WhileLoop(%d stmts)", body.size());
    }
}

