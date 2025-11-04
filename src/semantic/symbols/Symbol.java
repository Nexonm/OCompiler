package semantic.symbols;

import lexer.Span;

/**
 * Base class for all symbols in the symbol table.
 * A symbol represents a declared entity (variable, method, class, etc.)
 */
public abstract class Symbol {
    private final String name;
    private final String type;
    private final Span declaredAt;

    public Symbol(String name, String type, Span declaredAt) {
        this.name = name;
        this.type = type;
        this.declaredAt = declaredAt;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Span getDeclaredAt() {
        return declaredAt;
    }

    @Override
    public String toString() {
        return String.format("%s(name=%s, type=%s)",
                getClass().getSimpleName(), name, type);
    }
}
