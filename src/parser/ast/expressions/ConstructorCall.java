package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTNode;
import parser.ast.ASTVisitor;
import parser.ast.declarations.ClassDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a constructor call (object instantiation).
 * <p>
 * Examples:
 * <lu>
 * <li>Integer(42)</li>
 * <li>Real(3.14)</li>
 * <li>MyClass(arg1, arg2)</li>
 * </lu>
 * <p>
 * Grammar: ConstructorCall → ClassName ( [Arguments] )
 */
public class ConstructorCall extends Expression {
    private final String className;
    private final List<Expression> arguments;

    private ClassDecl resolvedClass = null;

    /**
     * Creates a constructor call node.
     *
     * @param className The name of the class being instantiated
     * @param arguments List of argument expressions (can be empty)
     * @param span      Position in source code
     */
    public ConstructorCall(String className, List<Expression> arguments, Span span) {
        super(span);
        this.className = className;
        this.arguments = new ArrayList<>(arguments); // Defensive copy
    }

    /**
     * Gets the class name being instantiated.
     *
     * @return Class name as string
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the list of constructor arguments.
     *
     * @return Unmodifiable list of argument expressions
     */
    public List<Expression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Gets the number of arguments.
     *
     * @return Argument count
     */
    public int getArgumentCount() {
        return arguments.size();
    }

    /**
     * Checks if the constructor call has no arguments.
     *
     * @return true if no arguments, false otherwise
     */
    public boolean hasNoArguments() {
        return arguments.isEmpty();
    }

    public ClassDecl getResolvedClass() {
        return resolvedClass;
    }

    public void setResolvedClass(ClassDecl resolvedClass) {
        this.resolvedClass = resolvedClass;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(
                String.format("ConstructorCall(%s, %d args)", className, arguments.size())
        );
        if (arguments.isEmpty()){
            return builder.toString();
        }else{
            builder.append("\n");
            for(ASTNode node: arguments){
                builder.append(node.toString()).append("\n");
            }
            return builder.toString();
        }
    }
}

