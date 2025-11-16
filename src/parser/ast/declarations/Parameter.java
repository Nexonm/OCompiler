package parser.ast.declarations;

import lexer.Span;
import semantic.types.Type;

/**
 * Represents a method or constructor parameter.
 * <p>
 * Grammar: {@code Parameter → Identifier : ClassName}
 * <p>
 * Example: {@code a : Integer, name : String}
 */
public class Parameter {
    private final String name;
    private final String typeName;
    private final Span span;

    private Type resolvedType = null;

    /**
     * Creates a parameter node.
     * @param name The parameter name
     * @param typeName The parameter type (class name)
     * @param span Position in source code
     */
    public Parameter(String name, String typeName, Span span) {
        this.name = name;
        this.typeName = typeName;
        this.span = span;
    }

    /**
     * @return Parameter identifier
     */
    public String getName() {
        return name;
    }

    /**
     * @return Type as string (class name)
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return Span object
     */
    public Span getSpan() {
        return span;
    }

    public Type getResolvedType() {
        return resolvedType;
    }

    public void setResolvedType(Type resolvedType) {
        this.resolvedType = resolvedType;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name, typeName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Parameter other)) return false;
        return name.equals(other.name) && typeName.equals(other.typeName);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + typeName.hashCode();
    }
}

