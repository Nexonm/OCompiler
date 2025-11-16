package semantic.types;

/**
 * Represents void/no return type.
 */
public class VoidType extends Type {
    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {}

    @Override
    public String getName() {
        return "void";
    }

    @Override
    public boolean isCompatibleWith(Type other) {
        return other instanceof VoidType;
    }

    @Override
    public String getJasminDescriptor() {
        return "V";
    }
}
