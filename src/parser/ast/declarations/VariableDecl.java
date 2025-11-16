package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.expressions.Expression;
import semantic.types.Type;

/**
 * Represents a variable declaration (class member).
 * <p>
 * Grammar: {@code }VariableDeclaration → var Identifier : Expression}
 * <p>
 * Example:
 * var x : Integer(42)
 * var name : String("default")
 * var count : Integer(0)
 * <p>
 * Note: In O language, variables are declared with an initializer.
 * The type is inferred from the initializer expression.
 */
public class VariableDecl extends MemberDecl {
    private final String name;
    private final Expression initializer;

    private Type declaredType = null;
    private boolean isParameter = false;
    private int localIndex = -1; // Used when isParameter=true, slots are: 1,2,3...

    /**
     * Creates a variable declaration node.
     * @param name The variable name
     * @param initializer The initialization expression
     * @param span Position in source code
     */
    public VariableDecl(String name, Expression initializer, Span span) {
        super(span);
        this.name = name;
        this.initializer = initializer;
    }

    /**
     * @return Variable identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the initializer expression.
     * @return Expression used to initialize this variable
     */
    public Expression getInitializer() {
        return initializer;
    }

    public Type getDeclaredType() {
        return declaredType;
    }

    public void setDeclaredType(Type declaredType) {
        this.declaredType = declaredType;
    }

    public int getLocalIndex() {
        return localIndex;
    }

    public void setLocalIndex(int localIndex) {
        this.localIndex = localIndex;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean parameter) {
        isParameter = parameter;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("VariableDecl(%s : %s)", name, initializer);
    }
}

