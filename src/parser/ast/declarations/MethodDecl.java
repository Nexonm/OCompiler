package parser.ast.declarations;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.statements.Statement;
import semantic.types.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a method declaration.
 * <p>
 * Grammar: {@code MethodDeclaration → MethodHeader [ MethodBody ]
 *          MethodHeader → method Identifier [ Parameters ] [ : Identifier ]
 *          MethodBody → is Body end | => Expression}
 * <p>
 * Examples:
 * <lu>
 *     <li>method foo()                       (no params, no return)</li>
 *     <li>method add(a : Integer) : Integer  (with params and return)</li>
 *     <li>method getValue() : Integer        (getter)</li>
 *     <li>method process() is ... end        (with body)</li>
 *     <li>method forward()                   (forward declaration)</li>
 * </lu>
 */
public class MethodDecl extends MemberDecl {
    private final String name;
    private final List<Parameter> parameters;
    private final String returnTypeName; // null if void
    private final List<Statement> body;  // null if forward declaration
    private final boolean isForward;

    private Type returnType = null;
    private String signature = null;
    private String jasminSignature = null; // for codegen

    /**
     * Creates a method declaration node.
     *
     * @param name The method name
     * @param parameters List of parameters (empty if no params)
     * @param returnTypeName Return type name (null if void/no return)
     * @param body List of statements in body (null if forward declaration)
     * @param span Position in source code
     */
    public MethodDecl(String name, List<Parameter> parameters,
                      String returnTypeName, List<Statement> body, Span span) {
        super(span);
        this.name = name;
        this.parameters = new ArrayList<>(parameters);
        this.returnTypeName = returnTypeName;
        this.body = body != null ? new ArrayList<>(body) : null;
        this.isForward = (body == null);
    }

    /**
     * Gets the method name.
     * @return Method identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the list of parameters.
     * @return Unmodifiable list of parameters
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Gets the number of parameters.
     * @return Parameter count
     */
    public int getParameterCount() {
        return parameters.size();
    }

    /**
     * Checks if method has no parameters.
     * @return true if no parameters, false otherwise
     */
    public boolean hasNoParameters() {
        return parameters.isEmpty();
    }

    /**
     * Gets the return type name.
     * @return Optional containing type name, or empty if void
     */
    public Optional<String> getReturnTypeName() {
        return Optional.ofNullable(returnTypeName);
    }

    /**
     * Gets the return type name (may be null).
     * @return Type name or null
     */
    public String getReturnTypeNameOrNull() {
        return returnTypeName;
    }

    /**
     * Checks if method has a return type.
     * @return true if return type specified, false if void
     */
    public boolean hasReturnType() {
        return returnTypeName != null;
    }

    /**
     * Checks if method returns void (no return type).
     * @return true if void, false otherwise
     */
    public boolean isVoid() {
        return returnTypeName == null;
    }

    /**
     * Gets the method body statements.
     * @return Optional containing list of statements, or empty if forward
     */
    public Optional<List<Statement>> getBody() {
        return Optional.ofNullable(body)
                .map(Collections::unmodifiableList);
    }

    /**
     * Gets the method body (may be null).
     * @return List of statements or null
     */
    public List<Statement> getBodyOrNull() {
        return body != null ? Collections.unmodifiableList(body) : null;
    }

    /**
     * Used to update the body in case of optimizations
     *
     * @param list new body
     */
    public void setBody(List<Statement> list) {
        if (body == null) {
            return;
        }
        body.clear();
        body.addAll(list);
    }

    /**
     * Checks if this is a forward declaration (no body).
     * @return true if forward, false if has body
     */
    public boolean isForwardDeclaration() {
        return isForward;
    }

    /**
     * Checks if this method has a body.
     * @return true if has body, false if forward
     */
    public boolean hasBody() {
        return !isForward;
    }

    /**
     * Gets the number of statements in body.
     * @return Statement count, or 0 if forward
     */
    public int getBodyStatementCount() {
        return body != null ? body.size() : 0;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public String getJasminSignature() {
        return jasminSignature;
    }

    public void setJasminSignature(String jasminSignature) {
        this.jasminSignature = jasminSignature;
    }

    /**
     * Compute method signature: "name(Type1,Type2,Type3)"
     * Example: "add(Integer,Integer)" or "getValue()"
     */
    public String computeSignature() {
        if (signature != null) return signature;

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = parameters.get(i);
            sb.append(param.getTypeName());
            if (i < parameters.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");

        signature = sb.toString();
        return signature;
    }

    public String getSignature() {
        if (signature == null) {
            computeSignature();
        }
        return signature;
    }

    public void setSignature(String sig) {
        this.signature = sig;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MethodDecl(").append(name);

        if (!parameters.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i));
            }
            sb.append(")");
        }

        if (returnTypeName != null) {
            sb.append(" : ").append(returnTypeName);
        }

        if (isForward) {
            sb.append(" [forward]");
        } else {
            sb.append(" [").append(getBodyStatementCount()).append(" statements]");
        }

        sb.append(")");
        return sb.toString();
    }
}

