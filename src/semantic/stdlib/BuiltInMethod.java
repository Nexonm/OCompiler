package semantic.stdlib;

import semantic.types.Type;
import java.util.List;

/**
 * Represents a built-in method signature.
 */
public class BuiltInMethod {
    private final String methodName;
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isBuiltIn;

    public BuiltInMethod(String methodName, List<Type> parameterTypes, Type returnType) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.isBuiltIn = true;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    /**
     * Get signature string: "methodName(Type1,Type2)"
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            sb.append(parameterTypes.get(i).getName());
            if (i < parameterTypes.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSignature() + " -> " + returnType.getName();
    }
}
