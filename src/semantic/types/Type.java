package semantic.types;

/**
 * Base class for all types in O language.
 */
public abstract class Type {

    public abstract String getName();

    /**
     * Check if this type is compatible with another type.
     * Used for assignment and method call type checking.
     */
    public abstract boolean isCompatibleWith(Type other);

    /**
     * Get Jasmin type descriptor.
     * Examples: "I" for int, "LInteger;" for Integer class
     */
    public abstract String getJasminDescriptor();

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Type other = (Type) obj;
        return getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
