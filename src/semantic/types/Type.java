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
}
