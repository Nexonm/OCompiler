package semantic.visitors;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.scope.GlobalScope;
import semantic.scope.LocalScope;
import semantic.scope.Scope;
import semantic.semantic.SemanticException;
import semantic.types.ArrayType;
import semantic.types.BuiltInTypes;
import semantic.types.ClassType;
import semantic.types.Type;

import java.util.*;

/**
 * First semantic pass: builds symbol tables and resolves names.
 * <p>
 * Three-pass strategy:
 * 1. Register all classes in global scope
 * 2. Build each class's member tables (fields + methods)
 * 3. Resolve all name references in method bodies
 */
public class SymbolTableBuilder implements ASTVisitor<Void> {

    private final GlobalScope globalScope;
    private final List<String> errors;

    // Current analysis context
    private Scope currentScope;
    private ClassDecl currentClass;
    private MethodDecl currentMethod;

    public SymbolTableBuilder() {
        this.globalScope = new GlobalScope();
        this.errors = new ArrayList<>();
        this.currentScope = globalScope;
    }

    /**
     * Main entry point: analyze entire program.
     */
    public void analyze(Program program) {
        // Pass 1: Register all classes
        registerClasses(program);

        // Pass 2: Build class member tables
        program.accept(this);

        // Report errors if any
        if (!errors.isEmpty()) {
            System.err.println("Symbol table building errors:");
            for (String error : errors) {
                System.err.println("  " + error);
            }
            throw new SemanticException(errors.size() + " symbol table errors found");
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public GlobalScope getGlobalScope() {
        return globalScope;
    }

    /**
     * Pass 1: Register all class declarations in global scope.
     */
    private void registerClasses(Program program) {
        // First, register built-in types
        globalScope.define("Integer", BuiltInTypes.INTEGER);
        globalScope.define("Boolean", BuiltInTypes.BOOLEAN);
        globalScope.define("Real", BuiltInTypes.REAL);

        // Register all user-defined classes
        for (ClassDecl classDecl : program.getClasses()) {
            try {
                // Create ClassType
                ClassType classType = new ClassType(classDecl.getName(), classDecl);
                classDecl.setClassType(classType);
                globalScope.define(classDecl.getName(), classDecl);
            } catch (SemanticException e) {
                errors.add(formatError("Duplicate class: " + classDecl.getName(),
                        classDecl.getSpan()));
            }
        }

        // Resolve inheritance
        for (ClassDecl classDecl : program.getClasses()) {
            if (classDecl.getBaseClassName().isPresent()) {
                String parentName = classDecl.getBaseClassName().get();
                if (parentName.equals(classDecl.getName())) {
                    errors.add(formatError(
                            "Class cannot extend itself: " + classDecl.getName(),
                            classDecl.getSpan()
                    ));
                    continue;  // Skip setting parent to avoid issues
                }

                Object parentObj = globalScope.resolve(parentName);
                if (parentObj == null) {
                    errors.add(formatError("Parent class not found: " + parentName,
                            classDecl.getSpan()));
                } else if (parentObj instanceof ClassDecl) {
                    classDecl.setParentClass((ClassDecl) parentObj);
                } else {
                    errors.add(formatError("Cannot extend built-in type: " + parentName,
                            classDecl.getSpan()));
                }
            }
        }

        for (ClassDecl classDecl : program.getClasses()) {
            if (classDecl.hasBaseClass()) {
                if (hasCircularInheritance(classDecl)) {
                    errors.add(formatError(
                            "Circular inheritance detected for class: " + classDecl.getName(),
                            classDecl.getSpan()
                    ));
                }
            }
        }
    }

    /**
     * Detect circular inheritance using visited set approach.
     */
    private boolean hasCircularInheritance(ClassDecl classDecl) {
        Set<ClassDecl> visited = new HashSet<>();
        ClassDecl current = classDecl;
        while (current != null) {
            if (visited.contains(current)) {
                return true;  // Found a cycle
            }
            visited.add(current);
            current = current.getParentClass();
        }
        return false;
    }

    // ========== VISITOR METHODS ==========

    @Override
    public Void visit(Program node) {
        // Pass 2: Build member tables for each class
        for (ClassDecl classDecl : node.getClasses()) {
            classDecl.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ClassDecl node) {
        currentClass = node;
        currentScope = node;  // ClassDecl implements Scope

        // Track constructors to check for duplicates
        Map<String, ConstructorDecl> constructors = new HashMap<>();

        // Separate fields and methods
        for (MemberDecl member : node.getMembers()) {
            if (member instanceof VariableDecl) {
                VariableDecl field = (VariableDecl) member;
                try {
                    node.addField(field);
                } catch (SemanticException e) {
                    errors.add(formatError("Duplicate field: " + field.getName(),
                            field.getSpan()));
                }
            } else if (member instanceof MethodDecl) {
                MethodDecl method = (MethodDecl) member;
                try {
                    node.addMethod(method);
                } catch (SemanticException e) {
                    errors.add(formatError("Duplicate method: " + method.getSignature(),
                            method.getSpan()));
                }
            } else if (member instanceof ConstructorDecl) {
                ConstructorDecl constructor = (ConstructorDecl) member;
                String signature = computeConstructorSignature(constructor);

                if (constructors.containsKey(signature)) {
                    errors.add(formatError(
                            "Duplicate constructor with signature: " + signature,
                            constructor.getSpan()
                    ));
                } else {
                    constructors.put(signature, constructor);
                }
            }
        }

        // Pass 3: Resolve names in methods
        for (MemberDecl member : node.getMembers()) {
            member.accept(this);
        }

        currentClass = null;
        currentScope = globalScope;
        return null;
    }

    /**
     * Compute constructor signature for duplicate detection.
     * Format: "this(Type1,Type2,Type3)"
     */
    private String computeConstructorSignature(ConstructorDecl constructor) {
        StringBuilder sb = new StringBuilder();
        sb.append("this(");

        List<Parameter> params = constructor.getParameters();
        for (int i = 0; i < params.size(); i++) {
            sb.append(params.get(i).getTypeName());
            if (i < params.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Void visit(MethodDecl node) {
        currentMethod = node;

        if (node.getReturnTypeName().isPresent()) {
            String returnTypeName = node.getReturnTypeName().get();
            Object typeObj = globalScope.resolve(returnTypeName);
            if (typeObj == null) {
                errors.add(formatError(
                        "Unknown return type: " + returnTypeName,
                        node.getSpan()
                ));
            }
        }

        // Create local scope for this method
        LocalScope methodScope = new LocalScope(currentScope);
        currentScope = methodScope;

        // Add parameters to scope
        for (Parameter param : node.getParameters()) {
            String paramTypeName = param.getTypeName();
            Type paramType = resolveParameterType(paramTypeName, param.getSpan());

            try {
                // Create VariableDecl for parameter (for uniform handling)
                VariableDecl paramDecl = new VariableDecl(
                        param.getName(),
                        null,  // parameters don't have initializers
                        param.getSpan()
                );
                paramDecl.setIsParameter(true);
                if (paramType != null) {
                    paramDecl.setDeclaredType(paramType);
                }

                methodScope.define(param.getName(), paramDecl);
            } catch (SemanticException e) {
                errors.add(formatError("Duplicate parameter: " + param.getName(),
                        param.getSpan()));
            }
        }

        // Visit method body
        if (node.getBody().isPresent()) {
            for (Statement stmt : node.getBody().get()) {
                stmt.accept(this);
            }
        }

        currentScope = currentScope.getEnclosingScope();
        currentMethod = null;
        return null;
    }

    /**
     * Resolve parameter type name to Type object.
     */
    private Type resolveParameterType(String typeName, Span span) {
        // Handle Array types
        if (typeName.startsWith("Array[") && typeName.endsWith("]")) {
            String innerName = typeName.substring(6, typeName.length() - 1);
            Type innerType = resolveParameterType(innerName, span);
            if (innerType != null) {
                return new ArrayType(innerType);
            }
            return null; // Error already reported in recursive call
        }

        // Check built-in types
        Type builtIn = BuiltInTypes.getBuiltInType(typeName);
        if (builtIn != null) {
            return builtIn;
        }

        // Check user-defined classes
        Object obj = globalScope.resolve(typeName);
        if (obj instanceof ClassDecl) {
            return ((ClassDecl) obj).getClassType();
        }

        errors.add(formatError("Unknown parameter type: " + typeName, span));
        return null;
    }

    @Override
    public Void visit(ConstructorDecl node) {
        // Similar to MethodDecl
        LocalScope constructorScope = new LocalScope(currentScope);
        currentScope = constructorScope;

        // Add parameters
        for (Parameter param : node.getParameters()) {
            try {
                VariableDecl paramDecl = new VariableDecl(
                        param.getName(),
                        null,
                        param.getSpan()
                );
                paramDecl.setIsParameter(true);
                constructorScope.define(param.getName(), paramDecl);
            } catch (SemanticException e) {
                errors.add(formatError("Duplicate parameter: " + param.getName(),
                        param.getSpan()));
            }
        }

        // Visit body
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }

        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    @Override
    public Void visit(VariableDecl node) {
        // Visit initializer
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        return null;
    }

    // ========== STATEMENTS ==========

    @Override
    public Void visit(VariableDeclStatement node) {
        VariableDecl varDecl = node.getVariableDecl();

        // Add to current scope
        try {
            currentScope.define(varDecl.getName(), varDecl);
        } catch (SemanticException e) {
            errors.add(formatError("Variable already declared: " + varDecl.getName(),
                    varDecl.getSpan()));
        }

        // Visit initializer
        varDecl.accept(this);
        return null;
    }

    @Override
    public Void visit(ExpressionStatement node) {
        node.getExpression().accept(this);
        return null;
    }

    @Override
    public Void visit(Assignment node) {
        // Resolve target variable
        Object symbol = currentScope.resolveRecursive(node.getTargetName());

        if (symbol == null) {
            errors.add(formatError("Undefined variable: " + node.getTargetName(),
                    node.getSpan()));
        } else if (symbol instanceof VariableDecl) {
            node.setResolvedTarget((VariableDecl) symbol);
        } else {
            errors.add(formatError("Cannot assign to: " + node.getTargetName(),
                    node.getSpan()));
        }

        // Visit value expression
        node.getValue().accept(this);
        return null;
    }

    @Override
    public Void visit(IfStatement node) {
        node.getCondition().accept(this);

        for (Statement stmt : node.getThenBranch()) {
            stmt.accept(this);
        }

        if (node.getElseBranch() != null) {
            for (Statement stmt : node.getElseBranch()) {
                stmt.accept(this);
            }
        }

        return null;
    }

    @Override
    public Void visit(WhileLoop node) {
        node.getCondition().accept(this);

        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(ReturnStatement node) {
        if (node.getValue().isPresent()) {
            node.getValue().get().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(UnknownStatement node) {
        // Skip unknown statements
        return null;
    }

    // ========== EXPRESSIONS ==========

    @Override
    public Void visit(IdentifierExpr node) {
        // Resolve identifier to declaration
        Object symbol = currentScope.resolveRecursive(node.getName());

        if (symbol == null) {
            errors.add(formatError("Undefined identifier: " + node.getName(),
                    node.getSpan()));
        } else if (symbol instanceof VariableDecl) {
            node.setResolvedDecl((VariableDecl) symbol);
        } else {
            errors.add(formatError("'" + node.getName() + "' is not a variable",
                    node.getSpan()));
        }

        return null;
    }

    @Override
    public Void visit(MethodCall node) {
        // Visit target expression
        node.getTarget().accept(this);

        // Visit arguments
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }

        // Method resolution happens in type checker (needs type info)
        return null;
    }

    @Override
    public Void visit(ConstructorCall node) {
        String className = node.getClassName();
        
        // Handle Array types
        if (className.startsWith("Array[") && className.endsWith("]")) {
            String innerName = className.substring(6, className.length() - 1);
            Type innerType = resolveParameterType(innerName, node.getSpan());
            if (innerType != null) {
                node.setResolvedType(new ArrayType(innerType));
            }
            
            // Visit arguments
            for (Expression arg : node.getArguments()) {
                arg.accept(this);
            }
            return null;
        }

        // Resolve class name
        Object classObj = globalScope.resolve(node.getClassName());

        if (classObj == null) {
            errors.add(formatError("Unknown class: " + node.getClassName(),
                    node.getSpan()));
        } else if (classObj instanceof ClassDecl) {
            node.setResolvedClass((ClassDecl) classObj);
        } else if (classObj instanceof Type) {
            // Built-in type (Integer, Boolean, etc.) - OK
        } else {
            errors.add(formatError("'" + node.getClassName() + "' is not a class",
                    node.getSpan()));
        }

        // Visit arguments
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(MemberAccess node) {
        // Visit object expression
        node.getTarget().accept(this);

        // Member resolution happens in type checker
        return null;
    }

    @Override
    public Void visit(ThisExpr node) {
        // 'this' is valid in instance methods
        if (currentMethod == null && currentClass == null) {
            errors.add(formatError("'this' cannot be used outside a method",
                    node.getSpan()));
        }
        return null;
    }

    @Override
    public Void visit(BooleanLiteral node) {
        // Literals need no resolution
        return null;
    }

    @Override
    public Void visit(IntegerLiteral node) {
        return null;
    }

    @Override
    public Void visit(RealLiteral node) {
        return null;
    }

    @Override
    public Void visit(UnknownExpression node) {
        return null;
    }

    // ========== UTILITY ==========

    /**
     * Format error message with source location.
     */
    private String formatError(String message, Span span) {
        return String.format("[%s] %s", span.toErrorString(), message);
    }
}

