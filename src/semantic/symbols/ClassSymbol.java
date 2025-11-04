package semantic.symbols;

import lexer.Span;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a class symbol with its members.
 */
public class ClassSymbol extends Symbol {
    private final String parentClassName;
    private final Map<String, VariableSymbol> fields;
    private final Map<String, MethodSymbol> methods;

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
        methods.put(method.getName(), method);
    }

    public Optional<VariableSymbol> getField(String name) {
        return Optional.ofNullable(fields.get(name));
    }

    public Optional<MethodSymbol> getMethod(String name) {
        return Optional.ofNullable(methods.get(name));
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name);
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

    @Override
    public String toString() {
        return String.format("ClassSymbol(name=%s, parent=%s, fields=%d, methods=%d)",
                getName(), parentClassName, fields.size(), methods.size());
    }
}

