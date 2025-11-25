package semantic.stdlib;

import semantic.types.BuiltInTypes;
import semantic.types.ClassType;
import semantic.types.Type;

import java.util.*;

/**
 * Registry of all built-in methods for Integer, Boolean, and Real types.
 * Based on O language specification.
 */
public class StandardLibrary {

    // Map: "ClassName.MethodSignature" -> BuiltInMethod
    private static final Map<String, BuiltInMethod> BUILT_IN_METHODS = new HashMap<>();

    static {
        initializeIntegerMethods();
        initializeBooleanMethods();
        initializeRealMethods();
    }

    /**
     * Initialize Integer class methods.
     */
    private static void initializeIntegerMethods() {
        Type intType = BuiltInTypes.INTEGER;
        Type boolType = BuiltInTypes.BOOLEAN;

        // Arithmetic operations
        register("Integer", "Plus", List.of(intType), intType);
        register("Integer", "Minus", List.of(intType), intType);
        register("Integer", "Mult", List.of(intType), intType);
        register("Integer", "Div", List.of(intType), intType);
        register("Integer", "Rem", List.of(intType), intType);

        // Unary operations
        register("Integer", "UnaryMinus", List.of(), intType);
        register("Integer", "UnaryPlus", List.of(), intType);

        // Comparison operations
        register("Integer", "Less", List.of(intType), boolType);
        register("Integer", "LessEqual", List.of(intType), boolType);
        register("Integer", "Greater", List.of(intType), boolType);
        register("Integer", "GreaterEqual", List.of(intType), boolType);
        register("Integer", "Equal", List.of(intType), boolType);

        // Conversion
        register("Integer", "toReal", List.of(), BuiltInTypes.REAL);
    }

    /**
     * Initialize Boolean class methods.
     */
    private static void initializeBooleanMethods() {
        Type boolType = BuiltInTypes.BOOLEAN;

        // Logical operations
        register("Boolean", "And", List.of(boolType), boolType);
        register("Boolean", "Or", List.of(boolType), boolType);
        register("Boolean", "Xor", List.of(boolType), boolType);
        register("Boolean", "Not", List.of(), boolType);
    }

    /**
     * Initialize Real class methods.
     */
    private static void initializeRealMethods() {
        Type realType = BuiltInTypes.REAL;
        Type boolType = BuiltInTypes.BOOLEAN;
        Type intType = BuiltInTypes.INTEGER;

        // Arithmetic operations
        register("Real", "Plus", List.of(realType), realType);
        register("Real", "Minus", List.of(realType), realType);
        register("Real", "Mult", List.of(realType), realType);
        register("Real", "Div", List.of(realType), realType);
        register("Real", "Rem", List.of(realType), realType);

        // Unary operations
        register("Real", "UnaryMinus", List.of(), realType);
        register("Real", "UnaryPlus", List.of(), realType);

        // Comparison operations
        register("Real", "Less", List.of(realType), boolType);
        register("Real", "LessEqual", List.of(realType), boolType);
        register("Real", "Greater", List.of(realType), boolType);
        register("Real", "GreaterEqual", List.of(realType), boolType);
        register("Real", "Equal", List.of(realType), boolType);

        // Conversion
        register("Real", "toInteger", List.of(), intType);
    }

    /**
     * Register a built-in method.
     */
    private static void register(String className, String methodName,
                                 List<Type> paramTypes, Type returnType) {
        BuiltInMethod method = new BuiltInMethod(methodName, paramTypes, returnType);
        String key = className + "." + method.getSignature();
        BUILT_IN_METHODS.put(key, method);
    }

    /**
     * Look up a built-in method.
     *
     * @param className The class name (Integer, Boolean, Real)
     * @param methodName The method name
     * @param argTypes The argument types
     * @return BuiltInMethod if found, null otherwise
     */
    public static BuiltInMethod findMethod(String className, String methodName,
                                           List<Type> argTypes) {
        // Handle Array methods
        if (className.startsWith("Array[") && className.endsWith("]")) {
            return findArrayMethod(className, methodName, argTypes);
        }

        // Build signature
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(methodName).append("(");
        for (int i = 0; i < argTypes.size(); i++) {
            if (argTypes.get(i) != null) {
                sigBuilder.append(argTypes.get(i).getName());
            } else {
                return null;  // Can't match if arg type is unknown
            }
            if (i < argTypes.size() - 1) {
                sigBuilder.append(",");
            }
        }
        sigBuilder.append(")");

        String key = className + "." + sigBuilder.toString();
        return BUILT_IN_METHODS.get(key);
    }

    private static BuiltInMethod findArrayMethod(String className, String methodName, List<Type> argTypes) {
        String innerName = className.substring(6, className.length() - 1);
        Type elementType;
        
        Type builtIn = BuiltInTypes.getBuiltInType(innerName);
        if (builtIn != null) {
            elementType = builtIn;
        } else {
            // For user types, create a placeholder ClassType (we only need the name for checking)
            elementType = new ClassType(innerName); 
        }

        if (methodName.equals("get")) {
            if (argTypes.size() == 1 && argTypes.get(0).getName().equals("Integer")) {
                return new BuiltInMethod("get", List.of(BuiltInTypes.INTEGER), elementType);
            }
        } else if (methodName.equals("set")) {
            if (argTypes.size() == 2 && 
                argTypes.get(0).getName().equals("Integer")) {
                // For the second argument, we accept if it's the element type (or compatible?)
                // TypeChecker handles compatibility. Here we just provide the definition.
                // But wait, if we return a method with signature (Integer, T), TypeChecker will match against it.
                return new BuiltInMethod("set", List.of(BuiltInTypes.INTEGER, elementType), BuiltInTypes.VOID);
            }
        } else if (methodName.equals("Length")) {
            if (argTypes.isEmpty()) {
                return new BuiltInMethod("Length", List.of(), BuiltInTypes.INTEGER);
            }
        }
        return null;
    }

    /**
     * Check if a type is a built-in type with methods.
     */
    public static boolean isBuiltInType(String typeName) {
        return typeName.equals("Integer") ||
                typeName.equals("Boolean") ||
                typeName.equals("Real") ||
                (typeName.startsWith("Array[") && typeName.endsWith("]"));
    }

    /**
     * Get all methods for a built-in class (for debugging/documentation).
     */
    public static List<BuiltInMethod> getMethodsForClass(String className) {
        List<BuiltInMethod> methods = new ArrayList<>();
        String prefix = className + ".";

        for (Map.Entry<String, BuiltInMethod> entry : BUILT_IN_METHODS.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                methods.add(entry.getValue());
            }
        }

        return methods;
    }
}
