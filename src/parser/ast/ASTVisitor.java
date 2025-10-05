package parser.ast;


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
    // Visit methods will be added as we implement more node types
    // For example:
    // T visitProgram(Program program);
    // T visitClassDecl(ClassDecl classDecl);
    // etc.
}

