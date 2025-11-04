package semantic.symbols;

import lexer.Span;

/**
 * Represents a variable or parameter symbol.
 */
public class VariableSymbol extends Symbol {
    private final boolean isParameter;
    private boolean isInitialized;

    public VariableSymbol(String name, String type, boolean isParameter, Span declaredAt) {
        super(name, type, declaredAt);
        this.isParameter = isParameter;
        // todo: check out if this is done in right way. I think `this.isInitialized = isParameter;` is right one.
        this.isInitialized = !isParameter; // params are considered initialized
    }

    public boolean isParameter() {
        return isParameter;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        this.isInitialized = initialized;
    }

    @Override
    public String toString() {
        return String.format("VariableSymbol(name=%s, type=%s, isParam=%b)",
                getName(), getType(), isParameter);
    }
}
