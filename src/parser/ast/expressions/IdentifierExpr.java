package parser.ast.expressions;
import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.VariableDecl;

/**
 * Represents an identifier (variable or class name) used in an expression.
 * <p>
 * Examples:
 * <lu>
 *     <li>Variable reference: x, myVar, result</li>
 *     <li>Class name (before constructor call): Integer, MyClass</li>
 * </lu>
 */
public class IdentifierExpr extends Expression {
    private final String name;

    private VariableDecl resolvedDecl = null;

    /**
     * Creates an identifier expression node.
     * @param name The identifier name
     * @param span Position in source code
     */
    public IdentifierExpr(String name, Span span) {
        super(span);
        this.name = name;
    }

    /**
     * @return The name as a string
     */
    public String getName() {
        return name;
    }

    public VariableDecl getResolvedDecl() {
        return resolvedDecl;
    }

    public void setResolvedDecl(VariableDecl resolvedDecl) {
        this.resolvedDecl = resolvedDecl;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("IdentifierExpr(%s)", name);
    }
}

