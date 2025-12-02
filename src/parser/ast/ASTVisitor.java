package parser.ast;

import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;


/**
 * Visitor interface for traversing the Abstract Syntax Tree.
 * This will be fully implemented in the semantic analysis phase.
 *
 * The Visitor pattern allows adding new operations on AST nodes
 * without modifying the node classes themselves.
 *
 * @param <T> The return type of visit operations
 */
public interface ASTVisitor<T> {
    // Program
    T visit(Program node);

    // Declarations
    T visit(ClassDecl node);
    T visit(MethodDecl node);
    T visit(ConstructorDecl node);
    T visit(VariableDecl node);

    // Expressions
    T visit(IdentifierExpr node);
    T visit(MethodCall node);
    T visit(ConstructorCall node);
    T visit(MemberAccess node);
    T visit(ThisExpr node);
    T visit(BooleanLiteral node);
    T visit(IntegerLiteral node);
    T visit(RealLiteral node);
    T visit(UnknownExpression node);

    // Statements
    T visit(Assignment node);
    T visit(IfStatement node);
    T visit(WhileLoop node);
    T visit(ReturnStatement node);
    T visit(VariableDeclStatement node);
    T visit(ExpressionStatement node);
    T visit(UnknownStatement node);
}

