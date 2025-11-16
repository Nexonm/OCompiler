package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.MethodDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method call on an object.
 * Grammar: MethodCall → Expression . Identifier ( Arguments )
 */
public class MethodCall extends Expression {
    private final Expression target;
    private final String methodName;
    private final List<Expression> arguments;

    private MethodDecl resolvedMethod = null;

    public MethodCall(Expression target, String methodName,
                      List<Expression> arguments, Span span) {
        super(span);
        this.target = target;
        this.methodName = methodName;
        this.arguments = new ArrayList<>(arguments);
    }

    /**
     * @return object the method call was done on
     */
    public Expression getTarget() {
        return target;
    }

    /**
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @return list of arguments in method call
     */
    public List<Expression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * @return number of passed arguments
     */
    public int getArgumentCount() {
        return arguments.size();
    }

    public MethodDecl getResolvedMethod() {
        return resolvedMethod;
    }

    public void setResolvedMethod(MethodDecl resolvedMethod) {
        this.resolvedMethod = resolvedMethod;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("MethodCall(%s.%s, %d args)",
                target, methodName, arguments.size());
    }
}
