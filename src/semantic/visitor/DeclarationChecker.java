package semantic.visitor;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.exception.SemanticException;
import semantic.symbols.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Semantic Check: Declaration Before Usage
 *
 * Verifies that all identifiers are declared before they are used.
 */
public class DeclarationChecker implements ASTVisitor<Void> {
    private final SymbolTable symbolTable;
    private final List<String> errors;

    public DeclarationChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>();
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
            System.err.println("\n=== Declaration Check Errors ===");
            for (String error : errors) {
                System.err.println(error);
            }
            System.err.println("=".repeat(50));
        }
    }

    // ========== PROGRAM & CLASS ==========

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

        // Register all variables and methods BEFORE checking method bodies
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof VariableDecl) {
                // Register the variable in the class scope
                VariableDecl varDecl = (VariableDecl) member;
                try {
                    symbolTable.define(new VariableSymbol(
                            varDecl.getName(),
                            varDecl.getInitializer().getClass().getTypeName(),
                            false,
                            varDecl.getSpan()
                    ));
                } catch (SemanticException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        // NOW check the method bodies (they can see class fields)
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


    // ========== MEMBERS ==========

    @Override
    public Void visitMethodDecl(MethodDecl methodDecl) {
        symbolTable.pushScope("method:" + methodDecl.getName());

        // Register parameters in the method scope
        for (Parameter param : methodDecl.getParameters()) {
            try {
                VariableSymbol paramSymbol = new VariableSymbol(
                        param.getName(),
                        param.getTypeName(),
                        true,  // is a parameter
                        param.getSpan()
                );
                symbolTable.define(paramSymbol);
            } catch (SemanticException e) {
                // Handle duplicate parameter
            }
        }

        // Now check the method body
        if (methodDecl.getBody().isPresent()) {
            for (Statement stmt : methodDecl.getBody().get()) {
                visitStatement(stmt);
            }
        }

        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitConstructorDecl(ConstructorDecl constructorDecl) {
        symbolTable.pushScope("constructor");

        // Register parameters
        for (Parameter param : constructorDecl.getParameters()) {
            try {
                VariableSymbol paramSymbol = new VariableSymbol(
                        param.getName(),
                        param.getTypeName(),
                        true,  // is a parameter
                        param.getSpan()
                );
                symbolTable.define(paramSymbol);
            } catch (SemanticException e) {
                // Handle duplicate parameter
            }
        }

        // Check the body
        for (Statement stmt : constructorDecl.getBody()) {
            visitStatement(stmt);
        }

        symbolTable.popScope();
        return null;
    }


    @Override
    public Void visitParameter(Parameter parameter) {
        return null;
    }

    @Override
    public Void visitVariableDecl(VariableDecl variableDecl) {
        return null;
    }

    // ========== STATEMENTS ==========

    @Override
    public Void visitAssignment(Assignment assignment) {
        String targetName = assignment.getTargetName();

        Optional<Symbol> symbol = symbolTable.lookup(targetName);
        if (symbol.isEmpty()) {
            addError(assignment.getSpan(), "Variable '" + targetName + "' is not declared");
        }

        visitExpression(assignment.getValue());
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement ifStmt) {
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
        visitExpression(loop.getCondition());

        for (Statement stmt : loop.getBody()) {
            visitStatement(stmt);
        }

        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement returnStmt) {
        if (returnStmt.getValue().isPresent()) {
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

    // ========== EXPRESSIONS ==========

    @Override
    public Void visitIdentifierExpr(IdentifierExpr expr) {
        String name = expr.getName();

        Optional<Symbol> symbol = symbolTable.lookup(name);
        if (symbol.isEmpty()) {
            addError(expr.getSpan(), "Identifier '" + name + "' is not declared");
        }
        return null;
    }

    @Override
    public Void visitMethodCall(MethodCall call) {
        visitExpression(call.getTarget());

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

        Optional<Symbol> symbol = symbolTable.lookup(className);
        if (symbol.isEmpty()) {
            addError(call.getSpan(), "Class '" + className + "' is not declared");
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
        // 'this' is valid anywhere (it's handled by semantic analysis layer)
        return null;
    }

    // ========== HELPER METHODS ==========

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
