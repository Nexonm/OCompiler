package semantic.types;

/**
 * Represents an Array type (e.g., Array[Integer]).
 */
public class ArrayType extends Type {
    private final Type elementType;

    public ArrayType(Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    @Override
    public String getName() {
        return "Array[" + elementType.getName() + "]";
    }

    @Override
    public boolean isCompatibleWith(Type other) {
        if (this == other) return true;
        if (other instanceof ArrayType) {
            // Exact match for element types (invariant)
            return elementType.equals(((ArrayType) other).elementType);
        }
        // Arrays are reference types
        return other.getName().equals("AnyRef");
    }

    @Override
    public String getJasminDescriptor() {
        return "[" + elementType.getJasminDescriptor();
    }
}


