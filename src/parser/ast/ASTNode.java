package parser.ast;

import lexer.Span;

/**
 * Base class for all Abstract Syntax Tree nodes.
 * Every node in the AST extends this class and carries position information.
 */
public abstract class ASTNode {
    private final Span span;

    /**
     * Creates a new AST node with position information.
     * @param span Position in source code where this node was parsed
     */
    public ASTNode(Span span) {
        this.span = span;
    }

    /**
     * @return Span object with line and column information
     */
    public Span getSpan() {
        return span;
    }

    /**
     * Visitor pattern support for AST traversal.
     * @param visitor The visitor to accept
     * @param <T> Return type of the visitor
     * @return Result of the visit operation
     */
    public abstract <T> T accept(ASTVisitor<T> visitor);

    /**
     * Creates a string representation for debugging.
     * Subclasses should override this to provide meaningful output.
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " at " + span;
    }
}

