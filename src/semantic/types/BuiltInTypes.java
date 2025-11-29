package semantic.types;

/**
 * Registry of built-in types.
 */
public class BuiltInTypes {
    public static final ClassType INTEGER = new ClassType("Integer");
    public static final ClassType BOOLEAN = new ClassType("Boolean");
    public static final ClassType REAL = new ClassType("Real");
    public static final ClassType PRINTER = new ClassType("Printer");
    public static final VoidType VOID = VoidType.INSTANCE;

    public static Type getBuiltInType(String name) {
        return switch (name) {
            case "Integer" -> INTEGER;
            case "Boolean" -> BOOLEAN;
            case "Real" -> REAL;
            case "Printer" -> PRINTER;
            default -> null;
        };
    }
}

