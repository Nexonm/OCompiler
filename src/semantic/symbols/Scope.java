package semantic.symbols;

import semantic.exception.SemanticException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single scope (visibility level) in the program.
 * Contains symbols defined at this level and a link to the parent scope.
 */
public class Scope {
    private final String name;
    private final Scope parent;
    private final Map<String, Symbol> symbols;

    public Scope(String name, Scope parent) {
        this.name = name;
        this.parent = parent;
        this.symbols = new HashMap<>();
    }

    /**
     * Define a new symbol in this scope.
     * @throws SemanticException if symbol already exists
     */
    public void define(Symbol symbol) {
        if (symbols.containsKey(symbol.getName())) {
            throw new SemanticException(
                    String.format("Symbol '%s' already defined in scope '%s'",
                            symbol.getName(), name)
            );
        }
        symbols.put(symbol.getName(), symbol);
    }

    /**
     * Resolve a symbol in THIS scope only (doesn't check parent).
     */
    public Optional<Symbol> resolve(String name) {
        return Optional.ofNullable(symbols.get(name));
    }

    /**
     * Resolve a symbol in this scope or any parent scope.
     */
    public Optional<Symbol> resolveRecursive(String name) {
        Optional<Symbol> symbol = resolve(name);
        if (symbol.isPresent()) {
            return symbol;
        }
        if (parent != null) {
            return parent.resolveRecursive(name);
        }
        return Optional.empty();
    }

    public String getName() {
        return name;
    }

    public Scope getParent() {
        return parent;
    }

    public boolean isGlobalScope() {
        return parent == null;
    }

    @Override
    public String toString() {
        return String.format("Scope(name=%s, symbols=%d, hasParent=%b)",
                name, symbols.size(), parent != null);
    }
}

