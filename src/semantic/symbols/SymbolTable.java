package semantic.symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Manages scopes and symbol resolution throughout semantic analysis.
 * Uses a stack to track nested scopes (global -> class -> method).
 */
public class SymbolTable {
    private final Deque<Scope> scopeStack;
    private Scope currentScope;

    public SymbolTable() {
        this.scopeStack = new ArrayDeque<>();
        // Always start with global scope
        pushScope("global");
    }

    /**
     * Enter a new scope (e.g., class or method).
     */
    public void pushScope(String scopeName) {
        Scope newScope = new Scope(scopeName, currentScope);
        scopeStack.push(newScope);
        currentScope = newScope;
    }

    /**
     * Exit the current scope.
     */
    public void popScope() {
        if (scopeStack.isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty scope stack");
        }
        scopeStack.pop();
        if (!scopeStack.isEmpty()) {
            currentScope = scopeStack.peek();
        } else {
            currentScope = null;
        }
    }

    /**
     * Define a symbol in the current scope.
     */
    public void define(Symbol symbol) {
        if (currentScope == null) {
            throw new IllegalStateException("No active scope");
        }
        currentScope.define(symbol);
    }

    /**
     * Look up a symbol starting from current scope,
     * searching up through parent scopes.
     */
    public Optional<Symbol> lookup(String name) {
        if (currentScope == null) {
            return Optional.empty();
        }
        return currentScope.resolveRecursive(name);
    }

    /**
     * Check if a symbol exists (shorthand for lookup().isPresent()).
     */
    public boolean isDefined(String name) {
        return lookup(name).isPresent();
    }

    /**
     * Get the current scope.
     */
    public Scope getCurrentScope() {
        return currentScope;
    }

    /**
     * Get the depth of the scope stack (0 = global only).
     */
    public int getScopeDepth() {
        return scopeStack.size();
    }

    /**
     * Check if we're in global scope.
     */
    public boolean isGlobalScope() {
        return scopeStack.size() == 1;
    }

    @Override
    public String toString() {
        return String.format("SymbolTable(depth=%d, current=%s)",
                scopeStack.size(), currentScope);
    }
}
