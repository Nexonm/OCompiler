package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTNode;

/**
 * Base class for class member declarations.
 * Class members can be:
 * - Variables ( {@code var x : Integer(5)})
 * - Methods ( {@code method foo() : Integer is ... end})
 * - Constructors ( {@code this() is ... end})
 */
public abstract class MemberDecl extends ASTNode {
    /**
     * Creates a new member declaration.
     * @param span Position in source code
     */
    public MemberDecl(Span span) {
        super(span);
    }
}

