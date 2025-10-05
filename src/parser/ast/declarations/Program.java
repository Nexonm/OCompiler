package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTNode;
import parser.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root node of the Abstract Syntax Tree.
 * Represents an entire O language program.
 *<p>
 * Grammar: Program → { ClassDeclaration }
 */
public class Program extends ASTNode {
    private final List<ClassDecl> classes;

    /**
     * Creates a program node with a list of classes.
     * @param classes List of class declarations (can be empty)
     */
    public Program(List<ClassDecl> classes) {
        super(Span.empty(0, 0)); // Program node has no specific location
        this.classes = new ArrayList<>(classes); // Defensive copy
    }

    /**
     * @return Unmodifiable list of classes
     */
    public List<ClassDecl> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    /**
     * Checks if the program is empty (no classes).
     * @return true if no classes defined, false otherwise
     */
    public boolean isEmpty() {
        return classes.isEmpty();
    }

    /**
     * Gets the number of classes in this program.
     * @return Number of class declarations
     */
    public int getClassCount() {
        return classes.size();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("Program(%d classes)", classes.size());
    }
}

