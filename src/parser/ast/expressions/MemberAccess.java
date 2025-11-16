package parser.ast.expressions;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.VariableDecl;

/**
 * Represents member access on an object.
 * Grammar: MemberAccess → Expression . Identifier
 */
public class MemberAccess extends Expression {
    private final Expression target;
    private final String memberName;

    private VariableDecl resolvedVar = null;

    public MemberAccess(Expression target, String memberName, Span span) {
        super(span);
        this.target = target;
        this.memberName = memberName;
    }

    /**
     * @return object the member access was done on
     */
    public Expression getTarget() {
        return target;
    }

    /**
     * @return member name
     */
    public String getMemberName() {
        return memberName;
    }

    public VariableDecl getResolvedVar() {
        return resolvedVar;
    }

    public void setResolvedVar(VariableDecl resolvedVar) {
        this.resolvedVar = resolvedVar;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("MemberAccess(%s.%s)", target, memberName);
    }
}
