package semantic.types;

import parser.ast.declarations.ClassDecl;

/**
 * Represents a class type (user-defined or built-in like Integer, Boolean).
 */
public class ClassType extends Type {
    private final String name;
    private final ClassDecl declaration;  // null for built-in types

    public ClassType(String name, ClassDecl declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    // Constructor for built-in types
    public ClassType(String name) {
        this(name, null);
    }

    @Override
    public String getName() {
        return name;
    }

    public ClassDecl getDeclaration() {
        return declaration;
    }

    public boolean isBuiltIn() {
        return declaration == null;
    }

    @Override
    public boolean isCompatibleWith(Type other) {
        if (!(other instanceof ClassType)) return false;

        ClassType otherClass = (ClassType) other;

        // Same class
        if (this.name.equals(otherClass.name)) return true;

        // Check inheritance (subclass can be assigned to parent)
        ClassDecl current = this.declaration;
        while (current != null && current.hasBaseClass()) {
            if (current.getParentClass().getName().equals(otherClass.declaration.getName())) {
                return true;
            }
            current = current.getParentClass();
        }

        return false;
    }

    @Override
    public String getJasminDescriptor() {
        return "L" + name + ";";
    }
}
