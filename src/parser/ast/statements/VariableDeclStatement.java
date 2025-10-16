package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.VariableDecl;

public class VariableDeclStatement extends Statement {
    private final VariableDecl variableDecl;

    public VariableDeclStatement(VariableDecl variableDecl, Span span) {
        super(span);
        this.variableDecl = variableDecl;
    }

    public VariableDecl getVariableDecl() {
        return variableDecl;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        // Will be implemented in semantic analysis phase
        return null;
    }

    @Override
    public String toString() {
        return String.format("VariableDeclStatement(%s)", variableDecl.toString());
    }
}
