package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTNode;
import semantic.types.Type;

/**
 * Base class for all expression nodes in the AST.
 *
 * Expressions represent values and computations in O language:
 * - Literals: 42, 3.14, true
 * - Identifiers: x, myVar
 * - Constructor calls: Integer(42)
 * - Method calls: obj.method(args)
 * - Member access: obj.member
 */
public abstract class Expression extends ASTNode {

    private Type inferredType = null;

    /**
     * Creates an expression node.
     * @param span Position in source code
     */
    public Expression(Span span) {
        super(span);
    }

    public Type getInferredType() {
        return inferredType;
    }

    public void setInferredType(Type inferredType) {
        this.inferredType = inferredType;
    }
}

