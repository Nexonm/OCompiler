package semantic.visitor;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.symbols.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Semantic Check: Type Consistency
 *
 * Verifies that:
 * 1. Assignments are type-compatible
 * 2. Method calls pass correct argument types
 * 3. Return statements match method return type
 * 4. Constructor calls are valid
 */
public class TypeConsistencyChecker implements ASTVisitor {
    private final SymbolTable symbolTable;
    private final List<String> errors;

    // Track current method/constructor context for return type checking
    private String currentMethodReturnType;
    private String currentMethodName;

    public TypeConsistencyChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>();
        this.currentMethodReturnType = null;
        this.currentMethodName = null;
    }

    public boolean check(Program program) {
        errors.clear();
        visitProgram(program);
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public void printErrors() {
        if (!errors.isEmpty()) {
            System.err.println("\n=== Type Consistency Check Errors ===");
            for (String error : errors) {
                System.err.println(error);
            }
            System.err.println("=".repeat(50));
        }
    }

    // ================== PROGRAM & CLASS ==================

    @Override
    public Void visitProgram(Program program) {
        for (ClassDecl classDecl : program.getClasses()) {
            visitClassDecl(classDecl);
        }
        return null;
    }

    @Override
    public Void visitClassDecl(ClassDecl classDecl) {
        symbolTable.pushScope("class:" + classDecl.getName());

        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof MethodDecl) {
                visitMethodDecl((MethodDecl) member);
            } else if (member instanceof ConstructorDecl) {
                visitConstructorDecl((ConstructorDecl) member);
            }
        }

        symbolTable.popScope();
        return null;
    }

    // ================== MEMBERS ==================

    @Override
    public Void visitMethodDecl(MethodDecl methodDecl) {
        symbolTable.pushScope("method:" + methodDecl.getName());

        // Register parameters in scope
        for (Parameter param : methodDecl.getParameters()) {
            visitParameter(param);
        }

        // Set context for return type checking
        currentMethodReturnType = methodDecl.getReturnTypeNameOrNull();
        currentMethodName = methodDecl.getName();

        // Check method body
        if (methodDecl.getBody().isPresent()) {
            for (Statement stmt : methodDecl.getBody().get()) {
                visitStatement(stmt);
            }
        }

        currentMethodReturnType = null;
        currentMethodName = null;
        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitConstructorDecl(ConstructorDecl constructorDecl) {
        symbolTable.pushScope("constructor");

        // Register parameters
        for (Parameter param : constructorDecl.getParameters()) {
            visitParameter(param);
        }

        // No return type for constructors
        currentMethodReturnType = null;

        // Check body
        for (Statement stmt : constructorDecl.getBody()) {
            visitStatement(stmt);
        }

        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitParameter(Parameter parameter) {
        try {
            VariableSymbol paramSymbol = new VariableSymbol(
                    parameter.getName(),
                    parameter.getTypeName(),
                    true,
                    parameter.getSpan()
            );
            symbolTable.define(paramSymbol);
        } catch (Exception e) {
            // Already defined, ignore
        }
        return null;
    }

    @Override
    public Void visitVariableDecl(VariableDecl variableDecl) {
        return null;
    }

    // ================== STATEMENTS ==================

    @Override
    public Void visitAssignment(Assignment assignment) {
        // Get target variable type
        String targetName = assignment.getTargetName();
        Optional<Symbol> targetSymbol = symbolTable.lookup(targetName);

        if (targetSymbol.isPresent() && targetSymbol.get() instanceof VariableSymbol) {
            VariableSymbol var = (VariableSymbol) targetSymbol.get();
            String expectedType = var.getType();

            // Infer value type
            String valueType = inferExpressionType(assignment.getValue());

            // Check compatibility (with type promotion)
            if (!isTypeCompatible(valueType, expectedType)) {
                addError(assignment.getSpan(),
                        String.format(
                                "Type mismatch in assignment: expected '%s', got '%s'",
                                expectedType, valueType
                        )
                );
            }
        }

        visitExpression(assignment.getValue());
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement ifStmt) {
        // Condition must be Boolean
        String conditionType = inferExpressionType(ifStmt.getCondition());
        if (!conditionType.equals("Boolean")) {
            addError(ifStmt.getSpan(),
                    String.format(
                            "If condition must be Boolean, got '%s'",
                            conditionType
                    )
            );
        }

        visitExpression(ifStmt.getCondition());
        for (Statement stmt : ifStmt.getThenBranch()) {
            visitStatement(stmt);
        }

        if (ifStmt.getElseBranch() != null) {
            for (Statement stmt : ifStmt.getElseBranch()) {
                visitStatement(stmt);
            }
        }

        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoop loop) {
        // Condition must be Boolean
        String conditionType = inferExpressionType(loop.getCondition());
        if (!conditionType.equals("Boolean")) {
            addError(loop.getSpan(),
                    String.format(
                            "While condition must be Boolean, got '%s'",
                            conditionType
                    )
            );
        }

        visitExpression(loop.getCondition());
        for (Statement stmt : loop.getBody()) {
            visitStatement(stmt);
        }

        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement returnStmt) {
        if (returnStmt.getValue().isPresent()) {
            String returnedType = inferExpressionType(returnStmt.getValue().get());

            // Check if return type matches method's declared return type
            if (currentMethodReturnType != null && !currentMethodReturnType.equals("void")) {
                if (!isTypeCompatible(returnedType, currentMethodReturnType)) {
                    addError(returnStmt.getSpan(),
                            String.format(
                                    "Return type mismatch in '%s': expected '%s', got '%s'",
                                    currentMethodName, currentMethodReturnType, returnedType
                            )
                    );
                }
            }

            visitExpression(returnStmt.getValue().get());
        }

        return null;
    }

    @Override
    public Void visitVariableDeclStatement(VariableDeclStatement stmt) {
        visitExpression(stmt.getVariableDecl().getInitializer());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement stmt) {
        visitExpression(stmt.getExpression());
        return null;
    }

    // ================== EXPRESSIONS ==================

    @Override
    public Void visitIdentifierExpr(IdentifierExpr expr) {
        // Already checked in DeclarationChecker
        return null;
    }

    @Override
    public Void visitMethodCall(MethodCall call) {
        // Get target type
        String targetType = inferExpressionType(call.getTarget());

        visitExpression(call.getTarget());

        // Look up method in target class
        Optional<Symbol> targetClassSymbol = symbolTable.lookup(targetType);

        if (targetClassSymbol.isPresent() && targetClassSymbol.get() instanceof ClassSymbol) {
            ClassSymbol targetClass = (ClassSymbol) targetClassSymbol.get();

            // Infer argument types
            List<String> argTypes = new ArrayList<>();
            for (Expression arg : call.getArguments()) {
                argTypes.add(inferExpressionType(arg));
            }

            // Look up method by name AND parameter types (supports overloading)
            Optional<MethodSymbol> method = targetClass.getMethod(call.getMethodName(), argTypes);

            if (method.isPresent()) {
                // Method found with matching signature - types are already compatible
                MethodSymbol methodSym = method.get();
            } else {
                // Try to find by name only for better error messages
                List<MethodSymbol> methodsByName = targetClass.getMethodsByName(call.getMethodName());

                if (methodsByName.isEmpty()) {
                    addError(call.getSpan(),
                            String.format(
                                    "Method '%s' not found in class '%s'",
                                    call.getMethodName(), targetType
                            )
                    );
                } else {
                    // Method exists but argument types don't match
                    addError(call.getSpan(),
                            String.format(
                                    "No matching overload for '%s' with argument types [%s] in class '%s'",
                                    call.getMethodName(),
                                    String.join(", ", argTypes),
                                    targetType
                            )
                    );
                }
            }
        }

        for (Expression arg : call.getArguments()) {
            visitExpression(arg);
        }

        return null;
    }

    @Override
    public Void visitMemberAccess(MemberAccess access) {
        visitExpression(access.getTarget());
        return null;
    }

    @Override
    public Void visitConstructorCall(ConstructorCall call) {
        String className = call.getClassName();
        Optional<Symbol> classSymbol = symbolTable.lookup(className);

        if (classSymbol.isPresent() && classSymbol.get() instanceof ClassSymbol) {
            ClassSymbol cls = (ClassSymbol) classSymbol.get();

            // Infer argument types for constructor
            List<String> argTypes = new ArrayList<>();
            for (Expression arg : call.getArguments()) {
                argTypes.add(inferExpressionType(arg));
            }

            // Look up constructor "this" with parameter types
            Optional<MethodSymbol> constructor = cls.getMethod("this", argTypes);

            if (constructor.isEmpty()) {
                // Try to find by name only for better error messages
                List<MethodSymbol> constructorsByName = cls.getMethodsByName("this");

                if (constructorsByName.isEmpty()) {
                    addError(call.getSpan(),
                            String.format(
                                    "No constructor found for class '%s'",
                                    className
                            )
                    );
                } else {
                    // Constructor exists but argument types don't match
                    addError(call.getSpan(),
                            String.format(
                                    "No matching constructor for '%s' with argument types [%s]",
                                    className,
                                    String.join(", ", argTypes)
                            )
                    );
                }
            }
        }

        for (Expression arg : call.getArguments()) {
            visitExpression(arg);
        }

        return null;
    }

    @Override
    public Void visitIntegerLiteral(IntegerLiteral literal) {
        return null;
    }

    @Override
    public Void visitBooleanLiteral(BooleanLiteral literal) {
        return null;
    }

    @Override
    public Void visitRealLiteral(RealLiteral literal) {
        return null;
    }

    @Override
    public Void visitThisExpr(ThisExpr expr) {
        return null;
    }

    // ================== HELPER METHODS ==================

    /**
     * Infer the type of an expression
     */
    private String inferExpressionType(Expression expr) {
        if (expr == null) return "Unknown";

        if (expr instanceof IntegerLiteral) {
            return "Integer";
        } else if (expr instanceof RealLiteral) {
            return "Real";
        } else if (expr instanceof BooleanLiteral) {
            return "Boolean";
        } else if (expr instanceof IdentifierExpr) {
            IdentifierExpr idExpr = (IdentifierExpr) expr;
            Optional<Symbol> symbol = symbolTable.lookup(idExpr.getName());
            if (symbol.isPresent() && symbol.get() instanceof VariableSymbol) {
                return ((VariableSymbol) symbol.get()).getType();
            }
            return "Unknown";
        } else if (expr instanceof ConstructorCall) {
            return ((ConstructorCall) expr).getClassName();
        }else if (expr instanceof MethodCall) {
            MethodCall call = (MethodCall) expr;
            String targetType = inferExpressionType(call.getTarget());

            Optional<Symbol> targetClass = symbolTable.lookup(targetType);
            if (targetClass.isPresent() && targetClass.get() instanceof ClassSymbol) {
                // Infer argument types
                List<String> argTypes = new ArrayList<>();
                for (Expression arg : call.getArguments()) {
                    argTypes.add(inferExpressionType(arg));
                }

                List<MethodSymbol> methodsWithName =
                        ((ClassSymbol) targetClass.get()).getMethodsByName(call.getMethodName());

                if (!methodsWithName.isEmpty()) {
                    // Try to find matching overload
                    MethodSymbol matchingMethod = null;
                    for (MethodSymbol method : methodsWithName) {
                        List<String> paramTypes = method.getParameterTypes();
                        if (paramTypes.size() == argTypes.size()) {
                            boolean allMatch = true;
                            for (int i = 0; i < paramTypes.size(); i++) {
                                if (!isTypeCompatible(argTypes.get(i), paramTypes.get(i))) {
                                    allMatch = false;
                                    break;
                                }
                            }
                            if (allMatch) {
                                matchingMethod = method;
                                break;
                            }
                        }
                    }

                    if (matchingMethod != null) {
                        String returnType = matchingMethod.getReturnType();
                        return returnType != null ? returnType : "void";
                    } else {
                        // No matching overload - but still return a type for inference
                        // The error will be caught in visitMethodCall()
                        MethodSymbol firstMethod = methodsWithName.get(0);
                        String returnType = firstMethod.getReturnType();
                        return returnType != null ? returnType : "void";
                    }
                }
            }
            return "Unknown";
        } else if (expr instanceof MemberAccess) {
            // For now, return the target type (simplified)
            return inferExpressionType(((MemberAccess) expr).getTarget());
        } else if (expr instanceof ThisExpr) {
            return "Unknown"; // Would need to track current class
        }
        return "Unknown";
    }

    /**
     * Check if two types are compatible
     */
    private boolean isTypeCompatible(String from, String to) {
        System.out.println("-----from: " + from + " to: " + to);
        // Exact match
        if (from.equals(to)) {
            return true;
        }

        // If either is Unknown, allow it (error reported elsewhere)
        if (from.equals("Unknown") || to.equals("Unknown")) {
            return true;
        }

        // No other implicit conversions allowed
        return false;
    }


    private void visitStatement(Statement stmt) {
        if (stmt instanceof VariableDeclStatement) {
            visitVariableDeclStatement((VariableDeclStatement) stmt);
        } else if (stmt instanceof Assignment) {
            visitAssignment((Assignment) stmt);
        } else if (stmt instanceof IfStatement) {
            visitIfStatement((IfStatement) stmt);
        } else if (stmt instanceof WhileLoop) {
            visitWhileLoop((WhileLoop) stmt);
        } else if (stmt instanceof ReturnStatement) {
            visitReturnStatement((ReturnStatement) stmt);
        } else if (stmt instanceof ExpressionStatement) {
            visitExpressionStatement((ExpressionStatement) stmt);
        }
    }

    private void visitExpression(Expression expr) {
        if (expr == null) return;

        if (expr instanceof IntegerLiteral) {
            visitIntegerLiteral((IntegerLiteral) expr);
        } else if (expr instanceof RealLiteral) {
            visitRealLiteral((RealLiteral) expr);
        } else if (expr instanceof BooleanLiteral) {
            visitBooleanLiteral((BooleanLiteral) expr);
        } else if (expr instanceof ThisExpr) {
            visitThisExpr((ThisExpr) expr);
        } else if (expr instanceof IdentifierExpr) {
            visitIdentifierExpr((IdentifierExpr) expr);
        } else if (expr instanceof ConstructorCall) {
            visitConstructorCall((ConstructorCall) expr);
        } else if (expr instanceof MethodCall) {
            visitMethodCall((MethodCall) expr);
        } else if (expr instanceof MemberAccess) {
            visitMemberAccess((MemberAccess) expr);
        }
    }

    private void addError(Span span, String message) {
        String errorMsg;
        if (span != null) {
            errorMsg = String.format("Line %d:%d - %s",
                    span.line(), span.start(), message);
        } else {
            errorMsg = message;
        }
        errors.add(errorMsg);
    }
}

