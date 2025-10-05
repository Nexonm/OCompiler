package parser.ast.statements;

import lexer.Span;
import parser.ast.ASTNode;

/**
 * Base class for all statement nodes in the AST.
 * <p>
 * Statements represent executable code in O language:
 * <lu>
 *     <li>Assignment: {@code x := expr}</li>
 *     <li>WhileLoop: {@code while expr loop ... end}</li>
 *     <li>IfStatement: {@code if expr then ... else ... end}</li>
 *     <li>ReturnStatement: {@code return expr}</li>
 * </lu>*/
public abstract class Statement extends ASTNode {

    /**
     * Creates a statement node.
     * @param span Position in source code
     */
    public Statement(Span span) {
        super(span);
    }
}

