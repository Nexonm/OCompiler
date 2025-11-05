package parser.ast;


import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

/**
 * Visitor interface for traversing the Abstract Syntax Tree.
 * <p>
 * The Visitor pattern allows adding new operations on AST nodes
 * without modifying the node classes themselves.
 *
 * @param <T> The return type of visit operations
 */
public interface ASTVisitor<T> {
    // Program
    T visitProgram(Program program);

    // Declarations
    T visitClassDecl(ClassDecl classDecl);
    T visitMethodDecl(MethodDecl methodDecl);
    T visitConstructorDecl(ConstructorDecl constructorDecl);
    T visitVariableDecl(VariableDecl variableDecl);
    T visitParameter(Parameter parameter);

    // Expressions
    T visitIdentifierExpr(IdentifierExpr expr);
    T visitMethodCall(MethodCall call);
    T visitMemberAccess(MemberAccess access);
    T visitConstructorCall(ConstructorCall call);
    T visitIntegerLiteral(IntegerLiteral literal);
    T visitBooleanLiteral(BooleanLiteral literal);
    T visitRealLiteral(RealLiteral literal);
    T visitThisExpr(ThisExpr expr);

    // Statements
    T visitAssignment(Assignment assignment);
    T visitIfStatement(IfStatement ifStmt);
    T visitWhileLoop(WhileLoop loop);
    T visitReturnStatement(ReturnStatement returnStmt);
    T visitVariableDeclStatement(VariableDeclStatement stmt);
    T visitExpressionStatement(ExpressionStatement stmt);
}

