package semantic.symbols;

import lexer.Span;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method or constructor symbol.
 */
public class MethodSymbol extends Symbol {
    private final List<String> parameterTypes;
    private final String returnType; // null for void or constructors
    private final boolean isConstructor;

    public MethodSymbol(String name, String returnType,
                        List<String> parameterTypes,
                        boolean isConstructor,
                        Span declaredAt) {
        super(name, returnType != null ? returnType : "void", declaredAt);
        this.returnType = returnType;
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.isConstructor = isConstructor;
    }

    public List<String> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public boolean hasReturnType() {
        return returnType != null;
    }

    public int getParameterCount() {
        return parameterTypes.size();
    }

    @Override
    public String toString() {
        return String.format("MethodSymbol(name=%s, params=%s, returns=%s, isCtor=%b)",
                getName(), parameterTypes, returnType, isConstructor);
    }
}

