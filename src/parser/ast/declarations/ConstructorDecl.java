package parser.ast.declarations;


import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.statements.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a constructor declaration.
 *
 * Grammar: ConstructorDeclaration → this [ Parameters ] is Body end
 *
 * Example:
 * this() is
 *     count := Integer(0)
 * end
 *
 * this(initialValue : Integer) is
 *     count := initialValue
 * end
 */
public class ConstructorDecl extends MemberDecl {
    private final List<Parameter> parameters;
    private final List<Statement> body;

    /**
     * Creates a constructor declaration node.
     *
     * @param parameters List of parameters (empty if no params)
     * @param body List of statements in constructor body
     * @param span Position in source code
     */
    public ConstructorDecl(List<Parameter> parameters,
                           List<Statement> body, Span span) {
        super(span);
        this.parameters = new ArrayList<>(parameters);
        this.body = new ArrayList<>(body);
    }

    /**
     * Gets the list of parameters.
     * @return Unmodifiable list of parameters
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Gets the number of parameters.
     * @return Parameter count
     */
    public int getParameterCount() {
        return parameters.size();
    }

    /**
     * Checks if constructor has no parameters.
     * @return true if no parameters, false otherwise
     */
    public boolean hasNoParameters() {
        return parameters.isEmpty();
    }

    /**
     * Gets the constructor body statements.
     * @return Unmodifiable list of statements
     */
    public List<Statement> getBody() {
        return Collections.unmodifiableList(body);
    }

    /**
     * Used to update the body in case of optimizations
     *
     * @param list new body
     */
    public void setBody(List<Statement> list) {
        if (body == null) {
            return;
        }
        body.clear();
        body.addAll(list);
    }

    /**
     * Gets the number of statements in body.
     * @return Statement count
     */
    public int getBodyStatementCount() {
        return body.size();
    }

    /**
     * Checks if body is empty.
     * @return true if no statements, false otherwise
     */
    public boolean hasEmptyBody() {
        return body.isEmpty();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConstructorDecl(");

        if (!parameters.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i));
            }
            sb.append(")");
        }

        sb.append(" [").append(getBodyStatementCount()).append(" statements]");
        sb.append(")");
        return sb.toString();
    }
}

