package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTNode;
import parser.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a class declaration in O language.
 * <p>
 * Grammar:
 * {@code ClassDeclaration → class Identifier [extends Identifier]
 * is { MemberDeclaration } end}
 * <p>
 * Example:
 * {@code
 * class Animal is
 * var name : String("Default")
 * method makeSound() : Integer is
 * return Integer(0)
 * end
 * end}
 * <p>
 * {@code
 * Example Result:
 * class Dog extends Animal is
 * // members...
 * end}
 */
public class ClassDecl extends ASTNode {
    private final String name;
    private final String baseClassName; // null if no inheritance
    private final List<MemberDecl> members;

    /**
     * Creates a class declaration node.
     *
     * @param name          The name of the class
     * @param baseClassName The name of the base class (null if no extends)
     * @param members       List of member declarations (variables, methods, constructors)
     * @param span          Position in source code
     */
    public ClassDecl(String name, String baseClassName,
                     List<MemberDecl> members, Span span) {
        super(span);
        this.name = name;
        this.baseClassName = baseClassName;
        this.members = new ArrayList<>(members); // Defensive copy
    }

    /**
     * Gets the class name.
     *
     * @return Class name identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the base class name if this class extends another.
     *
     * @return Optional containing base class name, or empty if no inheritance
     */
    public Optional<String> getBaseClassName() {
        return Optional.ofNullable(baseClassName);
    }

    /**
     * Checks if this class extends another class.
     *
     * @return true if extends clause present, false otherwise
     */
    public boolean hasBaseClass() {
        return baseClassName != null;
    }

    /**
     * Gets the base class name (may be null).
     *
     * @return Base class name or null
     */
    public String getBaseClassNameOrNull() {
        return baseClassName;
    }

    /**
     * Gets the list of member declarations.
     *
     * @return Unmodifiable list of members (variables, methods, constructors)
     */
    public List<MemberDecl> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * Gets the number of members in this class.
     *
     * @return Member count
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * Checks if the class has no members (empty class).
     *
     * @return true if no members, false otherwise
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        // Will be implemented in semantic analysis phase
        return null;
    }

    @Override
    public String toString() {
        String inheritance = hasBaseClass() ? " extends " + baseClassName : "";
        StringBuilder builder = new StringBuilder(
                String.format("ClassDecl(%s%s, %d members)", name, inheritance, members.size())
        );
        if (members.isEmpty()) {
            return builder.toString();
        } else {
            builder.append("\n");
            for (ASTNode node : members) {
                builder.append(node.toString()).append("\n");
            }
            return builder.toString();
        }
    }
}

