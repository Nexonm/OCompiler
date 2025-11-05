package semantic.symbols;

import lexer.Span;

import java.util.*;

/**
 * Represents a class symbol with its members.
 * Methods are stored by their signature (name + parameter types) to support overloading.
 */
public class ClassSymbol extends Symbol {
    private final String parentClassName;
    private final Map<String, VariableSymbol> fields;
    private final Map<String, MethodSymbol> methods; // Key: method signature

    public ClassSymbol(String name, String parentClassName, Span declaredAt) {
        super(name, name, declaredAt); // type is the class name itself
        this.parentClassName = parentClassName;
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    public void addField(VariableSymbol field) {
        fields.put(field.getName(), field);
    }

    public void addMethod(MethodSymbol method) {
        String signature = getMethodSignature(method);
        methods.put(signature, method);
    }

    public Optional<MethodSymbol> getMethod(String name, List<String> paramTypes) {
        String signature = buildSignature(name, paramTypes);
        return Optional.ofNullable(methods.get(signature));
    }

    public List<MethodSymbol> getMethodsByName(String name) {
        List<MethodSymbol> result = new ArrayList<>();
        for (MethodSymbol method : methods.values()) {
            if (method.getName().equals(name)) {
                result.add(method);
            }
        }
        return result;
    }

    public Optional<VariableSymbol> getField(String name) {
        return Optional.ofNullable(fields.get(name));
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public boolean hasMethod(String name) {
        for (MethodSymbol method : methods.values()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> getParentClassName() {
        return Optional.ofNullable(parentClassName);
    }

    public Map<String, VariableSymbol> getAllFields() {
        return new HashMap<>(fields);
    }

    public Map<String, MethodSymbol> getAllMethods() {
        return new HashMap<>(methods);
    }

    private String getMethodSignature(MethodSymbol method) {
        return buildSignature(method.getName(), method.getParameterTypes());
    }

    /**
     * Build method signature: "name(param1,param2,...)"
     * Example: "increment(Integer)" or "increment()"
     */
    private String buildSignature(String name, java.util.List<String> paramTypes) {
        StringBuilder sig = new StringBuilder(name).append("(");
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sig.append(",");
            sig.append(paramTypes.get(i));
        }
        sig.append(")");
        return sig.toString();
    }

    @Override
    public String toString() {
        return String.format("ClassSymbol(name=%s, parent=%s, fields=%d, methods=%d)",
                getName(), parentClassName, fields.size(), methods.size());
    }
}
