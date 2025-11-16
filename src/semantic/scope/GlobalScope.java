package semantic.scope;

import semantic.semantic.SemanticException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global scope containing all class declarations.
 */
public class GlobalScope implements Scope {
    private final Map<String, Object> symbols = new HashMap<>();

    @Override
    public Scope getEnclosingScope() {
        return null;  // No parent
    }

    @Override
    public void define(String name, Object symbol) throws SemanticException {
        if (symbols.containsKey(name)) {
            throw new SemanticException("Class already defined: " + name);
        }
        symbols.put(name, symbol);
    }

    @Override
    public Object resolve(String name) {
        return symbols.get(name);
    }
}
