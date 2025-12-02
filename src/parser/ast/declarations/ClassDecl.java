package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTNode;
import parser.ast.ASTVisitor;
import semantic.scope.Scope;
import semantic.semantic.SemanticException;
import semantic.types.ClassType;

import java.util.*;

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
public class ClassDecl extends ASTNode implements Scope {
    private final String name;
    private final String baseClassName; // null if no inheritance
    private final List<MemberDecl> members;

    private ClassDecl parentClass = null;
    private ClassType classType = null;
    private Map<String, MethodDecl> methodTable = null; // key = signature
    private Map<String, VariableDecl> fieldTable = null;

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

    /**
     * Find method by signature: "methodName(Type1,Type2)"
     */
    public MethodDecl findMethod(String signature) {
        if (methodTable != null && methodTable.containsKey(signature)) {
            return methodTable.get(signature);
        }
        // Search in parent class
        if (parentClass != null) {
            return parentClass.findMethod(signature);
        }
        return null;
    }

    /**
     * Find all methods with given name (for overload resolution)
     */
    public List<MethodDecl> findMethodsByName(String name) {
        List<MethodDecl> result = new ArrayList<>();
        if (methodTable != null) {
            for (Map.Entry<String, MethodDecl> entry : methodTable.entrySet()) {
                if (entry.getValue().getName().equals(name)) {
                    result.add(entry.getValue());
                }
            }
        }
        // Include parent class methods
        if (parentClass != null) {
            result.addAll(parentClass.findMethodsByName(name));
        }
        return result;
    }

    /**
     * Add method to table using its signature as key
     */
    public void addMethod(MethodDecl method) {
        if (methodTable == null) {
            methodTable = new HashMap<>();
        }
        String sig = method.getSignature();
        if (methodTable.containsKey(sig)) {
            MethodDecl existing = methodTable.get(sig);
            if (existing.isForwardDeclaration() && method.hasBody()) {
                // Replace forward declaration with implementation
                methodTable.put(sig, method);
                return;
            }
            throw new SemanticException("Duplicate method: " + sig);
        }
        methodTable.put(sig, method);
    }

    /**
     * Find field by name
     */
    public VariableDecl findField(String name) {
        if (fieldTable != null && fieldTable.containsKey(name)) {
            return fieldTable.get(name);
        }
        // Search in parent class
        if (parentClass != null) {
            return parentClass.findField(name);
        }
        return null;
    }

    /**
     * Add field to table
     */
    public void addField(VariableDecl field) {
        if (fieldTable == null) {
            fieldTable = new HashMap<>();
        }
        String name = field.getName();
        if (fieldTable.containsKey(name)) {
            throw new SemanticException("Duplicate field: " + name);
        }
        fieldTable.put(name, field);
    }

    public ClassDecl getParentClass() {
        return parentClass;
    }

    public void setParentClass(ClassDecl parentClass) {
        this.parentClass = parentClass;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType type) {
        this.classType = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        // Will be implemented in semantic analysis phase
        return visitor.visit(this);
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

    // Scope interface implementation:
    @Override
    public Scope getEnclosingScope() {
        return parentClass;  // Parent class is enclosing scope
    }

    @Override
    public void define(String name, Object symbol) throws SemanticException {
        if (symbol instanceof MethodDecl) {
            addMethod((MethodDecl) symbol);
        } else if (symbol instanceof VariableDecl) {
            addField((VariableDecl) symbol);
        }
    }

    @Override
    public Object resolve(String name) {
        // Try fields first
        if (fieldTable != null && fieldTable.containsKey(name)) {
            return fieldTable.get(name);
        }
        // Methods are resolved by signature, not name
        return null;
    }
}

