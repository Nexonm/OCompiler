package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;

import java.util.Optional;

/**
 * Represents a return statement.
 * <p>
 * Grammar: ReturnStatement → return [ Expression ]
 * <p>
 * Examples:
 * <lu>
 * <li>return              (void return)</li>
 * <li>return x            (return expression)</li>
 * <li>return x.Plus(y)    (return complex expression)</li>
 * </lu>
 */
public class ReturnStatement extends Statement {
    private Expression value; // null for void return // used for syntax, optimized at semantic stage

    /**
     * Creates a return statement node.
     *
     * @param value The return value expression (null for void)
     * @param span  Position in source code
     */
    public ReturnStatement(Expression value, Span span) {
        super(span);
        this.value = value;
    }

    /**
     * @return Optional containing expression, or empty if void return
     */
    public Optional<Expression> getValue() {
        return Optional.ofNullable(value);
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    /**
     * Checks if this is a void return (no value).
     *
     * @return true if no return value, false otherwise
     */
    public boolean isVoidReturn() {
        return value == null;
    }

    /**
     * @return Expression or null
     */
    public Expression getValueOrNull() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (isVoidReturn()) {
            return "ReturnStatement(void)";
        }
        return String.format("ReturnStatement(%s)", value);
    }
}

