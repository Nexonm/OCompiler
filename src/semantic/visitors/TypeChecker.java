package semantic.visitors;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.scope.GlobalScope;
import semantic.semantic.SemanticException;
import semantic.stdlib.BuiltInMethod;
import semantic.stdlib.StandardLibrary;
import semantic.types.ArrayType;
import semantic.types.BuiltInTypes;
import semantic.types.ClassType;
import semantic.types.Type;
import semantic.types.VoidType;

import java.util.ArrayList;
import java.util.List;

/**
 * Type checker: infers types and validates type compatibility.
 *
 * This is Pass 2 of semantic analysis, runs after SymbolTableBuilder.
 * Assumes all names are already resolved.
 */
public class TypeChecker implements ASTVisitor<Type> {

    private final GlobalScope globalScope;
    private final List<String> errors;
    private final List<String> warnings;

    // Current context
    private ClassDecl currentClass;
    private MethodDecl currentMethod;

    public TypeChecker(GlobalScope globalScope) {
        this.globalScope = globalScope;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Main entry point: type check entire program.
     */
    public void check(Program program) {
        program.accept(this);

        // Report results
        if (!warnings.isEmpty()) {
            System.err.println("Type checking warnings:");
            for (String warning : warnings) {
                System.err.println("  " + warning);
            }
        }

        if (!errors.isEmpty()) {
            System.err.println("Type checking errors:");
            for (String error : errors) {
                System.err.println("  " + error);
            }
            throw new SemanticException(errors.size() + " type errors found");
        }

        System.out.println("Type checking passed!");
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    // ========== VISITOR METHODS ==========

    @Override
    public Type visit(Program node) {
        for (ClassDecl classDecl : node.getClasses()) {
            classDecl.accept(this);
        }
        return null;
    }

    @Override
    public Type visit(ClassDecl node) {
        currentClass = node;

        // PASS 1: Resolve all method signatures (return types + parameter types)
        for (MemberDecl member : node.getMembers()) {
            if (member instanceof MethodDecl) {
                MethodDecl method = (MethodDecl) member;
                resolveMethodSignature(method);
            } else if (member instanceof ConstructorDecl) {
                ConstructorDecl ctor = (ConstructorDecl) member;
                resolveConstructorSignature(ctor);
            }
        }

        // PASS 2: Type check all member bodies
        for (MemberDecl member : node.getMembers()) {
            member.accept(this);
        }

        currentClass = null;
        return null;
    }

    // ========== MEMBER VISITORS ==========

    @Override
    public Type visit(MethodDecl node) {
        currentMethod = node;
        // Signature resolved at pass 1
        // Type check method body
        if (node.getBody().isPresent()) {
            for (Statement stmt : node.getBody().get()) {
                stmt.accept(this);
            }
        }

        currentMethod = null;
        return null;
    }

    @Override
    public Type visit(ConstructorDecl node) {
        // Resolve parameter types
        for (Parameter param : node.getParameters()) {
            Type paramType = resolveTypeName(param.getTypeName(), param.getSpan());
            param.setResolvedType(paramType);
        }

        // Type check constructor body
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }

        return null;
    }

    @Override
    public Type visit(VariableDecl node) {
        // Infer type from initializer
        if (node.getInitializer() != null) {
            Type initType = node.getInitializer().accept(this);
            node.setDeclaredType(initType);
            return initType;
        }
        return null;
    }

    // ========== STATEMENTS ==========

    @Override
    public Type visit(VariableDeclStatement node) {
        node.getVariableDecl().accept(this);
        return null;
    }

    @Override
    public Type visit(ExpressionStatement node) {
        return node.getExpression().accept(this);
    }

    @Override
    public Type visit(Assignment node) {
        // Get target variable type
        VariableDecl target = node.getResolvedTarget();
        if (target == null) {
            // Error already reported by SymbolTableBuilder
            return null;
        }

        Type targetType = target.getDeclaredType();
        if (targetType == null) {
            errors.add(formatError(
                    "Cannot determine type of variable: " + target.getName(),
                    node.getSpan()
            ));
            return null;
        }

        // Get value type
        Type valueType = node.getValue().accept(this);
        if (valueType == null) {
            errors.add(formatError(
                    "Cannot determine type of expression in assignment to: " + target.getName(),
                    node.getValue().getSpan()
            ));
            return null;
        }

        // Check compatibility
        if (!valueType.isCompatibleWith(targetType)) {
            errors.add(formatError(
                    String.format("Type mismatch in assignment for var %s: cannot assign %s to %s",
                            target.getName(), valueType.getName(), targetType.getName()),
                    node.getSpan()
            ));
        }

        return null;
    }

    @Override
    public Type visit(IfStatement node) {
        // Check condition type
        Type conditionType = node.getCondition().accept(this);

        if (conditionType != null && !conditionType.equals(BuiltInTypes.BOOLEAN)) {
            errors.add(formatError(
                    "'If' condition must be Boolean, got: " + conditionType.getName(),
                    node.getCondition().getSpan()
            ));
        }

        // Type check branches
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
    public Type visit(WhileLoop node) {
        // Check condition type
        Type conditionType = node.getCondition().accept(this);

        if (conditionType != null && !conditionType.equals(BuiltInTypes.BOOLEAN)) {
            errors.add(formatError(
                    "'While' condition must be Boolean, got: " + conditionType.getName(),
                    node.getCondition().getSpan()
            ));
        }

        // Type check body
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }

        return null;
    }

    @Override
    public Type visit(ReturnStatement node) {
        if (currentMethod == null) {
            // Return outside method - should be caught elsewhere
            return null;
        }

        Type expectedReturnType = currentMethod.getReturnType();

        if (node.getValue().isEmpty()) {
            // Return with no value
            if (expectedReturnType != null && !expectedReturnType.equals(VoidType.INSTANCE)) {
                errors.add(formatError(
                        "Method must return a value of type: " + expectedReturnType.getName(),
                        node.getSpan()
                ));
            }
        } else {
            // Return with value
            Type actualReturnType = node.getValue().get().accept(this);
            if (expectedReturnType == null || expectedReturnType.equals(VoidType.INSTANCE)) {
                // todo: actually, void returns are acceptable. Check this clause in case it disagrees
                errors.add(formatError(
                        "Method should not return a value",
                        node.getSpan()
                ));
            } else
                if (actualReturnType != null &&
                    !actualReturnType.isCompatibleWith(expectedReturnType)) {
                errors.add(formatError(
                        String.format("Return type mismatch: expected %s, got %s",
                                expectedReturnType.getName(), actualReturnType.getName()),
                        node.getSpan()
                ));
            }
        }

        return null;
    }

    @Override
    public Type visit(UnknownStatement node) {
        return null;
    }

    // ========== EXPRESSIONS ==========

    @Override
    public Type visit(IdentifierExpr node) {
        VariableDecl decl = node.getResolvedDecl();
        if (decl == null) {
            // Error already reported by SymbolTableBuilder
            return null;
        }

        Type type = decl.getDeclaredType();
        node.setInferredType(type);
        return type;
    }

    @Override
    public Type visit(ConstructorCall node) {
        // Get class type
        String className = node.getClassName();
        Type classType = resolveTypeName(className, node.getSpan());

        if (classType == null) {
            return null;
        }

        // Type check arguments
        List<Type> argTypes = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            Type argType = arg.accept(this);
            argTypes.add(argType);
        }

        if (StandardLibrary.isBuiltInType(className)) {
            // Built-in types have specific constructor signatures
            validateBuiltInConstructor(className, argTypes, node.getSpan());
        } else {
            // User-defined classes - check constructor exists
            validateUserConstructor(node, className, argTypes);
        }

        // Return the constructed type
        node.setInferredType(classType);
        return classType;
    }

    /**
     * Validate built-in type constructor calls.
     */
    private void validateBuiltInConstructor(String className, List<Type> argTypes, Span span) {
        if (className.startsWith("Array[")) {
             // Array constructor expects 1 argument (Integer size)
             if (argTypes.size() != 1) {
                    errors.add(formatError(
                            "Array constructor expects 1 argument (size), got " + argTypes.size(),
                            span
                    ));
             } else if (argTypes.get(0) != null &&
                        !argTypes.get(0).equals(BuiltInTypes.INTEGER)) {
                 errors.add(formatError(
                            "Array constructor size must be Integer",
                            span
                 ));
             }
             return;
        }

        // Built-in types expect specific argument types
        switch (className) {
            case "Integer":
                // Integer(Integer) or Integer(int literal)
                if (argTypes.size() != 1) {
                    errors.add(formatError(
                            "Integer constructor expects 1 argument, got " + argTypes.size(),
                            span
                    ));
                } else if (argTypes.get(0) != null &&
                        !argTypes.get(0).equals(BuiltInTypes.INTEGER)) {
                    errors.add(formatError(
                            String.format("Integer constructor expects Integer argument, got %s",
                                    argTypes.get(0).getName()),
                            span
                    ));
                }
                break;

            case "Boolean":
                // Boolean(Boolean) or Boolean(bool literal)
                if (argTypes.size() != 1) {
                    errors.add(formatError(
                            "Boolean constructor expects 1 argument, got " + argTypes.size(),
                            span
                    ));
                } else if (argTypes.get(0) != null &&
                        !argTypes.get(0).equals(BuiltInTypes.BOOLEAN)) {
                    errors.add(formatError(
                            String.format("Boolean constructor expects Boolean argument, got %s",
                                    argTypes.get(0).getName()),
                            span
                    ));
                }
                break;

            case "Real":
                // Real(Real) or Real(real literal)
                if (argTypes.size() != 1) {
                    errors.add(formatError(
                            "Real constructor expects 1 argument, got " + argTypes.size(),
                            span
                    ));
                } else if (argTypes.get(0) != null &&
                        !argTypes.get(0).equals(BuiltInTypes.REAL)) {
                    errors.add(formatError(
                            String.format("Real constructor expects Real argument, got %s",
                                    argTypes.get(0).getName()),
                            span
                    ));
                }
                break;
        }
    }

    /**
     * Validate user-defined class constructor calls.
     */
    private void validateUserConstructor(ConstructorCall node, String className,
                                         List<Type> argTypes) {
        ClassDecl classDecl = node.getResolvedClass();

        if (classDecl == null) {
            return;  // Error already reported
        }

        // Find matching constructor
        // Similar to method resolution but for constructors
        ConstructorDecl constructor = findConstructor(classDecl, argTypes, node.getSpan());

        if (constructor == null) {
            errors.add(formatError(
                    String.format("No matching constructor for %s with %d arguments",
                            className, argTypes.size()),
                    node.getSpan()
            ));
        }
    }

    /**
     * Find constructor matching argument types.
     */
    private ConstructorDecl findConstructor(ClassDecl classDecl, List<Type> argTypes,
                                            Span callSpan) {
        // Get all constructors from class
        List<ConstructorDecl> constructors = new ArrayList<>();
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof ConstructorDecl) {
                constructors.add((ConstructorDecl) member);
            }
        }

        // Find one with matching parameters
        for (ConstructorDecl ctor : constructors) {
            if (ctor.getParameters().size() == argTypes.size()) {
                boolean matches = true;

                for (int i = 0; i < argTypes.size(); i++) {
                    Type paramType = ctor.getParameters().get(i).getResolvedType();
                    Type argType = argTypes.get(i);

                    if (argType != null && paramType != null &&
                            !argType.isCompatibleWith(paramType)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    return ctor;
                }
            }
        }

        return null;
    }


    @Override
    public Type visit(MethodCall node) {
        // Explicit restriction on literals
        if (node.getTarget() instanceof IntegerLiteral ||
                node.getTarget() instanceof RealLiteral ||
                node.getTarget() instanceof BooleanLiteral) {
            errors.add(formatError("Cannot call method on literal directly", node.getTarget().getSpan()));
            return null;
        }

        // Get target type
        Type targetType = node.getTarget().accept(this);

        if (targetType == null) {
            errors.add(formatError(
                    "Cannot determine type of method call target",
                    node.getTarget().getSpan()
            ));
            return null;
        }

        // Type check arguments
        List<Type> argTypes = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            Type argType = arg.accept(this);
            argTypes.add(argType);
        }

        if (targetType instanceof ArrayType) {
            return handleArrayMethodCall(node, (ArrayType) targetType, argTypes);
        }

        // Check if this is a built-in type
        if (StandardLibrary.isBuiltInType(targetType.getName())) {
            return handleBuiltInMethodCall(node, targetType, argTypes);
        }

        return handleUserDefinedMethodCall(node, targetType, argTypes);
    }


    /**
     * Handle method calls on built-in types (Integer, Boolean, Real).
     */
    private Type handleBuiltInMethodCall(MethodCall node, Type targetType,
                                         List<Type> argTypes) {
        String className = targetType.getName();
        String methodName = node.getMethodName();

        // Look up built-in method
        BuiltInMethod builtInMethod = StandardLibrary.findMethod(className, methodName, argTypes);

        if (builtInMethod == null) {
            errors.add(formatError(
                    String.format("No built-in method %s.%s with %d arguments",
                            className, methodName, argTypes.size()),
                    node.getSpan()
            ));
            return null;
        }

        // Store return type
        Type returnType = builtInMethod.getReturnType();
        node.setInferredType(returnType);

        return returnType;
    }

    /**
     * Handle method calls on array types.
     */
    private Type handleArrayMethodCall(MethodCall node, ArrayType arrayType,
                                       List<Type> argTypes) {
        String methodName = node.getMethodName();
        Type elementType = arrayType.getElementType();

        switch (methodName) {
            case "get": {
                if (argTypes.size() != 1) {
                    errors.add(formatError(
                            "Array.get expects 1 argument, got " + argTypes.size(),
                            node.getSpan()
                    ));
                    return null;
                }
                Type indexType = argTypes.get(0);
                if (indexType != null && !indexType.equals(BuiltInTypes.INTEGER)) {
                    errors.add(formatError(
                            "Array.get index must be Integer",
                            node.getArguments().get(0).getSpan()
                    ));
                    return null;
                }
                node.setInferredType(elementType);
                return elementType;
            }
            case "set": {
                if (argTypes.size() != 2) {
                    errors.add(formatError(
                            "Array.set expects 2 arguments, got " + argTypes.size(),
                            node.getSpan()
                    ));
                    return null;
                }
                Type indexType = argTypes.get(0);
                if (indexType != null && !indexType.equals(BuiltInTypes.INTEGER)) {
                    errors.add(formatError(
                            "Array.set index must be Integer",
                            node.getArguments().get(0).getSpan()
                    ));
                    return null;
                }
                Type valueType = argTypes.get(1);
                if (valueType != null && !valueType.isCompatibleWith(elementType)) {
                    errors.add(formatError(
                            String.format("Array.set value type mismatch: expected %s, got %s",
                                    elementType.getName(), valueType.getName()),
                            node.getArguments().get(1).getSpan()
                    ));
                    return null;
                }
                node.setInferredType(VoidType.INSTANCE);
                return VoidType.INSTANCE;
            }
            case "Length": {
                if (!argTypes.isEmpty()) {
                    errors.add(formatError(
                            "Array.Length expects 0 arguments",
                            node.getSpan()
                    ));
                    return null;
                }
                node.setInferredType(BuiltInTypes.INTEGER);
                return BuiltInTypes.INTEGER;
            }
            default:
                errors.add(formatError(
                        "Unknown array method: " + methodName,
                        node.getSpan()
                ));
                return null;
        }
    }

    /**
     * Handle method calls on user-defined classes.
     */
    private Type handleUserDefinedMethodCall(MethodCall node, Type targetType,
                                             List<Type> argTypes) {
        // Get class declaration from type
        ClassDecl targetClass = null;
        if (targetType instanceof ClassType) {
            targetClass = ((ClassType) targetType).getDeclaration();
        }

        if (targetClass == null) {
            errors.add(formatError(
                    "Cannot call methods on type: " + targetType.getName(),
                    node.getSpan()
            ));
            return null;
        }

        // Find matching method
        MethodDecl method = findMethod(targetClass, node.getMethodName(), argTypes, node.getSpan());

        if (method == null) {
            return null;  // Error already reported
        }

        // Link method call to resolved method
        node.setResolvedMethod(method);

        // Get return type
        Type returnType = method.getReturnType();

        if (returnType == null) {
            errors.add(formatError(
                    "Method return type not resolved: " + method.getName(),
                    node.getSpan()
            ));
            return null;
        }

        // Store and return type
        node.setInferredType(returnType);
        return returnType;
    }

    @Override
    public Type visit(MemberAccess node) {
        // Get object type
        Type objectType = node.getTarget().accept(this);

        if (objectType == null) {
            return null;
        }

        // Get class declaration
        ClassDecl objectClass = null;
        if (objectType instanceof ClassType) {
            objectClass = ((ClassType) objectType).getDeclaration();
        }

        //todo: we should be able to access members
        if (objectClass == null) {
            errors.add(formatError(
                    "Cannot access members of built-in type: " + objectType.getName(),
                    node.getSpan()
            ));
            return null;
        }

        // Find field
        VariableDecl field = objectClass.findField(node.getMemberName());

        if (field == null) {
            errors.add(formatError(
                    String.format("Class %s has no field: %s",
                            objectClass.getName(), node.getMemberName()),
                    node.getSpan()
            ));
            return null;
        }

        Type fieldType = field.getDeclaredType();
        node.setInferredType(fieldType);
        return fieldType;
    }

    @Override
    public Type visit(ThisExpr node) {
        if (currentClass == null) {
            errors.add(formatError("'this' used outside class context", node.getSpan()));
            return null;
        }

        Type thisType = currentClass.getClassType();
        node.setInferredType(thisType);
        return thisType;
    }

    @Override
    public Type visit(BooleanLiteral node) {
        node.setInferredType(BuiltInTypes.BOOLEAN);
        return BuiltInTypes.BOOLEAN;
    }

    @Override
    public Type visit(IntegerLiteral node) {
        node.setInferredType(BuiltInTypes.INTEGER);
        return BuiltInTypes.INTEGER;
    }

    @Override
    public Type visit(RealLiteral node) {
        node.setInferredType(BuiltInTypes.REAL);
        return BuiltInTypes.REAL;
    }

    @Override
    public Type visit(UnknownExpression node) {
        return null;
    }

    // ========== HELPER METHODS ==========

    /**
     * Resolve type name to Type object.
     */
    private Type resolveTypeName(String typeName, Span span) {
        // Handle Array types
        if (typeName.startsWith("Array[") && typeName.endsWith("]")) {
            String innerName = typeName.substring(6, typeName.length() - 1);
            Type innerType = resolveTypeName(innerName, span);
            if (innerType != null) {
                return new ArrayType(innerType);
            }
            return null;
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
        errors.add(formatError("Unknown type: " + typeName, span));
        return null;
    }

    /**
     * Find method by name and argument types (handles overloading).
     */
    private MethodDecl findMethod(ClassDecl classDecl, String methodName,
                                  List<Type> argTypes, Span callSpan) {
        // Build signature
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(methodName).append("(");
        for (int i = 0; i < argTypes.size(); i++) {
            if (argTypes.get(i) != null) {
                sigBuilder.append(argTypes.get(i).getName());
            } else {
                sigBuilder.append("?");
            }
            if (i < argTypes.size() - 1) {
                sigBuilder.append(",");
            }
        }
        sigBuilder.append(")");
        String signature = sigBuilder.toString();

        // Try exact match
        MethodDecl method = classDecl.findMethod(signature);

        if (method != null) {
            return method;
        }

        // No exact match - try to find by name and check parameter compatibility
        List<MethodDecl> candidates = classDecl.findMethodsByName(methodName);

        for (MethodDecl candidate : candidates) {
            if (candidate.getParameters().size() == argTypes.size()) {
                boolean compatible = true;
                for (int i = 0; i < argTypes.size(); i++) {
                    Type paramType = candidate.getParameters().get(i).getResolvedType();
                    Type argType = argTypes.get(i);

                    if (argType != null && paramType != null &&
                            !argType.isCompatibleWith(paramType)) {
                        compatible = false;
                        break;
                    }
                }

                if (compatible) {
                    return candidate;
                }
            }
        }

        // No match found
        errors.add(formatError(
                String.format("No matching method found: %s in class %s",
                        signature, classDecl.getName()),
                callSpan
        ));
        return null;
    }


    /**
     * Resolve method signature without checking body.
     */
    private void resolveMethodSignature(MethodDecl method) {
        // Resolve parameter types
        for (Parameter param : method.getParameters()) {
            Type paramType = resolveTypeName(param.getTypeName(), param.getSpan());
            param.setResolvedType(paramType);
        }

        // Resolve return type
        if (method.getReturnTypeName().isPresent()) {
            Type returnType = resolveTypeName(method.getReturnTypeName().get(), method.getSpan());
            method.setReturnType(returnType);
        } else {
            method.setReturnType(VoidType.INSTANCE);
        }
    }

    /**
     * Resolve constructor signature without checking body.
     */
    private void resolveConstructorSignature(ConstructorDecl ctor) {
        // Resolve parameter types
        for (Parameter param : ctor.getParameters()) {
            Type paramType = resolveTypeName(param.getTypeName(), param.getSpan());
            param.setResolvedType(paramType);
        }
    }


    /**
     * Format error message with source location.
     */
    private String formatError(String message, Span span) {
        return String.format("[%s] %s", span.toErrorString(), message);
    }
}

