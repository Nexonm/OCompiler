package semantic.scope;

import semantic.semantic.SemanticException;

/**
 * Represents a lexical scope for name resolution.
 */
public interface Scope {

    /**
     * Get the enclosing (parent) scope.
     */
    Scope getEnclosingScope();

    /**
     * Define a symbol in this scope.
     * @throws SemanticException if symbol already exists
     */
    void define(String name, Object symbol) throws SemanticException;

    /**
     * Look up symbol in this scope only.
     */
    Object resolve(String name);

    /**
     * Look up symbol in this scope and all enclosing scopes.
     */
    default Object resolveRecursive(String name) {
        Object symbol = resolve(name);
        if (symbol != null) {
            return symbol;
        }
        if (getEnclosingScope() != null) {
            return getEnclosingScope().resolveRecursive(name);
        }
        return null;
    }
}

