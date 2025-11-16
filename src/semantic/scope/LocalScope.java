package semantic.scope;

import semantic.semantic.SemanticException;

import java.util.HashMap;
import java.util.Map;

/**
 * Local scope for method parameters and local variables.
 */
public class LocalScope implements Scope {
    private final Scope enclosingScope;
    private final Map<String, Object> symbols = new HashMap<>();

    public LocalScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(String name, Object symbol) throws SemanticException {
        if (symbols.containsKey(name)) {
            throw new SemanticException("Variable already defined in this scope: " + name);
        }
        symbols.put(name, symbol);
    }

    @Override
    public Object resolve(String name) {
        return symbols.get(name);
    }
}

