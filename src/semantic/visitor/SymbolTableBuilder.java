package semantic.visitor;

import lexer.Span;
import parser.ast.ASTNode;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.symbols.*;
import semantic.exception.*;
import semantic.symbols.*;

import java.util.ArrayList;
import java.util.List;

/**
 * THE MAIN VISITOR!
 * First pass visitor: Builds the symbol table by registering all declarations.
 * Walks the AST and creates symbols for classes, methods, variables, and parameters.
 */
public class SymbolTableBuilder implements ASTVisitor<Void> {
    private final SymbolTable symbolTable;
    private final List<String> errors;
    private ClassSymbol currentClass; // Track which class we're inside

    public SymbolTableBuilder() {
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
        this.currentClass = null;
    }

    /**
     * Perform symbol table construction on the program.
     * @return true if successful (no errors), false otherwise
     */
    public boolean build(Program program) {
        errors.clear();
        visitProgram(program);
        return errors.isEmpty();
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public void printErrors() {
        if (!errors.isEmpty()) {
            System.err.println("=== Symbol Table Build Errors ===");
            for (String error : errors) {
                System.err.println(error);
            }
        }
    }

    // ****************** Main Logic *******************

    // ==================== Program ====================

    /**
     * Basically, starts the program parsing. It is used to scan the whole program.
     * <p>
     * <lu>
     *     <li>1. Scan all class declarations</li>
     *     <li>2. Scan all inside classes</li>
     * </lu>
     * @param program the main {@link ASTNode} node of the program
     * @return nothing
     */
    @Override
    public Void visitProgram(Program program) {
        // First pass: Register all class names
        for (ClassDecl classDecl : program.getClasses()) {
            try {
                ClassSymbol classSymbol = new ClassSymbol(
                        classDecl.getName(),
                        classDecl.getBaseClassNameOrNull(),
                        classDecl.getSpan()
                );
                symbolTable.define(classSymbol);
            } catch (SemanticException e) {
                errors.add(formatError(classDecl.getSpan(), e.getMessage()));
            }
        }
        // Second pass: Process class bodies
        for (ClassDecl classDecl : program.getClasses()) {
            visitClassDecl(classDecl);
        }
        return null;
    }

    // ==================== Declarations ====================

    @Override
    public Void visitClassDecl(ClassDecl classDecl) {
        // Enter class scope
        symbolTable.pushScope("class:" + classDecl.getName());
        // Get the class symbol we created in first pass
        currentClass = (ClassSymbol) symbolTable.lookup(classDecl.getName()).orElse(null);
        if (currentClass == null) {
            errors.add(formatError(classDecl.getSpan(),
                    "Internal error: Class symbol not found for " + classDecl.getName()));
            symbolTable.popScope();
            return null;
        }
        // Process all members
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof VariableDecl) {
                visitVariableDecl((VariableDecl) member);
            } else if (member instanceof MethodDecl) {
                visitMethodDecl((MethodDecl) member);
            } else if (member instanceof ConstructorDecl) {
                visitConstructorDecl((ConstructorDecl) member);
            }
        }
        // Exit class scope
        currentClass = null;
        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitVariableDecl(VariableDecl variableDecl) {
        try {
            // Infer type from initializer
            String type = inferType(variableDecl.getInitializer());
            VariableSymbol varSymbol = new VariableSymbol(
                    variableDecl.getName(),
                    type,
                    false, // not a parameter
                    variableDecl.getSpan()
            );
            symbolTable.define(varSymbol);
            if (currentClass != null) { // Usually we are in class, but check to avoid NullPointerException
                currentClass.addField(varSymbol);
            }
        } catch (SemanticException e) {
            errors.add(formatError(variableDecl.getSpan(), e.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitMethodDecl(MethodDecl methodDecl) {
        try {
            List<String> paramTypes = new ArrayList<>();
            for (Parameter param : methodDecl.getParameters()) {
                paramTypes.add(param.getTypeName());
            }
            MethodSymbol methodSymbol = new MethodSymbol(
                    methodDecl.getName(),
                    methodDecl.getReturnTypeNameOrNull(),
                    paramTypes,
                    false, // not a constructor
                    methodDecl.getSpan()
            );
            if (currentClass != null) { // Usually we are in class, but check to avoid NullPointerException
                currentClass.addMethod(methodSymbol);
            }
            symbolTable.pushScope("method:" + methodDecl.getName());
            // Register parameters in method scope
            for (Parameter param : methodDecl.getParameters()) {
                visitParameter(param);
            }
            // Process method body (for local variable declarations)
            if (methodDecl.getBody().isPresent()) {
                for (Statement stmt : methodDecl.getBody().get()) {
                    visitStatement(stmt);
                }
            }
            symbolTable.popScope();
        } catch (SemanticException e) {
            errors.add(formatError(methodDecl.getSpan(), e.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitConstructorDecl(ConstructorDecl constructorDecl) {
        try {
            // Collect parameter types
            List<String> paramTypes = new ArrayList<>();
            for (Parameter param : constructorDecl.getParameters()) {
                paramTypes.add(param.getTypeName());
            }
            // Create constructor symbol (name is "this")
            MethodSymbol constructorSymbol = new MethodSymbol(
                    "this",
                    null, // constructors don't have return type
                    paramTypes,
                    true, // is a constructor
                    constructorDecl.getSpan()
            );
            if (currentClass != null) { // Usually we are in class, but check to avoid NullPointerException
                currentClass.addMethod(constructorSymbol);
            }
            symbolTable.pushScope("constructor");
            // Register parameters
            for (Parameter param : constructorDecl.getParameters()) {
                visitParameter(param);
            }
            // Process body (for local variable declarations)
            for (Statement stmt : constructorDecl.getBody()) {
                visitStatement(stmt);
            }
            symbolTable.popScope();
        } catch (SemanticException e) {
            errors.add(formatError(constructorDecl.getSpan(), e.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitParameter(Parameter parameter) {
        try {
            VariableSymbol paramSymbol = new VariableSymbol(
                    parameter.getName(),
                    parameter.getTypeName(),
                    true, // is a parameter
                    parameter.getSpan()
            );
            symbolTable.define(paramSymbol);
        } catch (SemanticException e) {
            errors.add(formatError(parameter.getSpan(), e.getMessage()));
        }
        return null;
    }

    // ==================== Statements ====================

    @Override
    public Void visitVariableDeclStatement(VariableDeclStatement stmt) {
        try {
            String type = inferType(stmt.getVariableDecl().getInitializer());
            VariableSymbol varSymbol = new VariableSymbol(
                    stmt.getVariableDecl().getName(),
                    type,
                    false,
                    stmt.getSpan()
            );
            symbolTable.define(varSymbol);
        } catch (SemanticException e) {
            errors.add(formatError(stmt.getSpan(), e.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitAssignment(Assignment assignment) {
        // Nothing to do for symbol table building
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement ifStmt) {
        // Process then-branch for local variable declarations
        for (Statement stmt : ifStmt.getThenBranch()) {
            visitStatement(stmt);
        }
        // Process else-branch if present
        if (ifStmt.getElseBranch() != null) {
            for (Statement stmt : ifStmt.getElseBranch()) {
                visitStatement(stmt);
            }
        }
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoop loop) {
        // Process body for local variable declarations
        for (Statement stmt : loop.getBody()) {
            visitStatement(stmt);
        }
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement returnStmt) {
        // Nothing to do for symbol table building
        return null;
    }

    // ==================== Expressions (not used for building) ====================

    @Override
    public Void visitIdentifierExpr(IdentifierExpr expr) {
        return null;
    }

    @Override
    public Void visitMethodCall(MethodCall call) {
        return null;
    }

    @Override
    public Void visitMemberAccess(MemberAccess access) {
        return null;
    }

    @Override
    public Void visitConstructorCall(ConstructorCall call) {
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

    // ==================== Helper Methods ====================

    /**
     * Infer the type of expression (simplified version).
     * For now, just handle constructor calls.
     */
    private String inferType(Expression expr) {
        if (expr instanceof ConstructorCall) {
            return ((ConstructorCall) expr).getClassName();
        } else if (expr instanceof IntegerLiteral) {
            return "Integer";
        } else if (expr instanceof BooleanLiteral) {
            return "Boolean";
        } else if (expr instanceof RealLiteral) {
            return "Real";
        }
        return "Unknown";
    }

    /**
     * Dispatch to appropriate visit method based on statement type.
     */
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
        }
        // Add other statement types as needed
    }

    /**
     * Format an error message with location info.
     */
    private String formatError(Span span, String message) {
        if (span != null) {
            return String.format("Line %d:%d: %s",
                    span.line(), span.start(), message);
        }
        return message;
    }
}

