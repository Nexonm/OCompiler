package semantic.visitors;

import lexer.Span;
import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

import java.util.List;

/**
 * Constant Folder - evaluates constant expressions at compile time.
 *
 * Handles:
 * - Integer arithmetic: Plus, Minus, Mult, Div, Rem
 * - Integer comparisons: Less, Greater, Equal, etc.
 * - Boolean operations: And, Or, Xor, Not
 * - Real arithmetic: Plus, Minus, Mult, Div
 *
 * Example transformations:
 *   Integer(5).Plus(Integer(3)) → Integer(8)
 *   Boolean(true).And(Boolean(false)) → Boolean(false)
 *   Integer(10).Greater(Integer(5)) → Boolean(true)
 */
public class ConstantFolder implements ASTVisitor<Expression> {

    private int expressionsFolded = 0;
    private boolean changed = false;

    public ConstantFolder() {
    }

    /**
     * Run optimization on entire program.
     * Returns true if any changes were made.
     */
    public boolean optimize(Program program) {
        expressionsFolded = 0;
        changed = false;
        program.accept(this);

        if (expressionsFolded > 0) {
            System.out.println("Constant folding: simplified " + expressionsFolded +
                    " expression(s)");
        }

        return changed;
    }

    public int getExpressionsFolded() {
        return expressionsFolded;
    }

    // ========== VISITOR METHODS ==========

    @Override
    public Expression visit(Program node) {
        for (ClassDecl classDecl : node.getClasses()) {
            classDecl.accept(this);
        }
        return null;
    }

    @Override
    public Expression visit(ClassDecl node) {
        for (MemberDecl member : node.getMembers()) {
            member.accept(this);
        }
        return null;
    }

    @Override
    public Expression visit(MethodDecl node) {
        if (node.getBody().isPresent()) {
            for (Statement stmt : node.getBody().get()) {
                stmt.accept(this);
            }
        }
        return null;
    }

    @Override
    public Expression visit(ConstructorDecl node) {
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public Expression visit(VariableDecl node) {
        if (node.getInitializer() != null) {
            Expression folded = node.getInitializer().accept(this);
            if (folded != null && folded != node.getInitializer()) {
                node.setInitializer(folded);
                changed = true;
            }
        }
        return null;
    }

    // ========== STATEMENTS ==========

    @Override
    public Expression visit(VariableDeclStatement node) {
        node.getVariableDecl().accept(this);
        return null;
    }

    @Override
    public Expression visit(Assignment node) {
        Expression folded = node.getValue().accept(this);
        if (folded != null && folded != node.getValue()) {
            node.setValue(folded);
            changed = true;
        }
        return null;
    }

    @Override
    public Expression visit(IfStatement node) {
        // Fold condition
        Expression foldedCond = node.getCondition().accept(this);
        if (foldedCond != null && foldedCond != node.getCondition()) {
            node.setCondition(foldedCond);
            changed = true;
        }

        // Fold branches
        for (Statement stmt : node.getThenBranch()) {
            stmt.accept(this);
        }
        if (node.getElseBranch() != null) {
            for (Statement stmt : node.getElseBranch()) {
                stmt.accept(this);
            }
        }
        return null;
    }

    @Override
    public Expression visit(WhileLoop node) {
        // Fold condition
        Expression foldedCond = node.getCondition().accept(this);
        if (foldedCond != null && foldedCond != node.getCondition()) {
            node.setCondition(foldedCond);
            changed = true;
        }

        // Fold body
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public Expression visit(ReturnStatement node) {
        if (node.getValue().isPresent()) {
            Expression folded = node.getValue().get().accept(this);
            if (folded != null && folded != node.getValue().get()) {
                node.setValue(folded);
                changed = true;
            }
        }
        return null;
    }

    @Override
    public Expression visit(UnknownStatement node) {
        return null;
    }

    // ========== EXPRESSIONS ==========

    @Override
    public Expression visit(MethodCall node) {
        // First, recursively fold target and arguments
        Expression foldedTarget = node.getTarget().accept(this);
        if (foldedTarget != null && foldedTarget != node.getTarget()) {
            node.setTarget(foldedTarget);
            changed = true;
        }

        for (int i = 0; i < node.getArguments().size(); i++) {
            Expression arg = node.getArguments().get(i);
            Expression foldedArg = arg.accept(this);
            if (foldedArg != null && foldedArg != arg) {
                node.getArguments().set(i, foldedArg);
                changed = true;
            }
        }

        // Now try to fold this method call if it's on constants
        Expression folded = tryFoldMethodCall(node);
        if (folded != null) {
            expressionsFolded++;
            changed = true;
            return folded;
        }

        return node;
    }

    /**
     * Try to fold a method call on constant values.
     */
    private Expression tryFoldMethodCall(MethodCall call) {
        Expression target = call.getTarget();
        String methodName = call.getMethodName();

        // Check if target is a constant (ConstructorCall with literal arg)
        Integer targetInt = extractIntegerConstant(target);
        Boolean targetBool = extractBooleanConstant(target);
        Double targetReal = extractRealConstant(target);

        // Integer operations
        if (targetInt != null) {
            if (call.getArguments().size() == 1) {
                Integer argInt = extractIntegerConstant(call.getArguments().get(0));
                if (argInt != null) {
                    return foldIntegerBinaryOp(targetInt, argInt, methodName, call.getSpan());
                }
            } else if (call.getArguments().isEmpty()) {
                return foldIntegerUnaryOp(targetInt, methodName, call.getSpan());
            }
        }

        // Boolean operations
        if (targetBool != null) {
            if (call.getArguments().size() == 1) {
                Boolean argBool = extractBooleanConstant(call.getArguments().get(0));
                if (argBool != null) {
                    return foldBooleanBinaryOp(targetBool, argBool, methodName, call.getSpan());
                }
            } else if (call.getArguments().isEmpty()) {
                return foldBooleanUnaryOp(targetBool, methodName, call.getSpan());
            }
        }

        // Real operations
        if (targetReal != null) {
            if (call.getArguments().size() == 1) {
                Double argReal = extractRealConstant(call.getArguments().get(0));
                if (argReal != null) {
                    return foldRealBinaryOp(targetReal, argReal, methodName, call.getSpan());
                }
            } else if (call.getArguments().isEmpty()) {
                return foldRealUnaryOp(targetReal, methodName, call.getSpan());
            }
        }

        return null;  // Can't fold
    }

    /**
     * Fold Integer binary operations.
     */
    private Expression foldIntegerBinaryOp(int left, int right, String op, Span span) {
        return switch (op) {
            case "Plus" -> createIntegerLiteral(left + right, span);
            case "Minus" -> createIntegerLiteral(left - right, span);
            case "Mult" -> createIntegerLiteral(left * right, span);
            case "Div" -> right != 0 ? createIntegerLiteral(left / right, span) : null;
            case "Rem" -> right != 0 ? createIntegerLiteral(left % right, span) : null;
            case "Less" -> createBooleanLiteral(left < right, span);
            case "LessEqual" -> createBooleanLiteral(left <= right, span);
            case "Greater" -> createBooleanLiteral(left > right, span);
            case "GreaterEqual" -> createBooleanLiteral(left >= right, span);
            case "Equal" -> createBooleanLiteral(left == right, span);
            default -> null;
        };
    }

    /**
     * Fold Integer unary operations.
     */
    private Expression foldIntegerUnaryOp(int value, String op, Span span) {
        return switch (op) {
            case "UnaryMinus" -> createIntegerLiteral(-value, span);
            case "UnaryPlus" -> createIntegerLiteral(value, span);
            default -> null;
        };
    }

    /**
     * Fold Boolean binary operations.
     */
    private Expression foldBooleanBinaryOp(boolean left, boolean right, String op, Span span) {
        return switch (op) {
            case "And" -> createBooleanLiteral(left && right, span);
            case "Or" -> createBooleanLiteral(left || right, span);
            case "Xor" -> createBooleanLiteral(left ^ right, span);
            default -> null;
        };
    }

    /**
     * Fold Boolean unary operations.
     */
    private Expression foldBooleanUnaryOp(boolean value, String op, Span span) {
        if (op.equals("Not")) {
            return createBooleanLiteral(!value, span);
        }
        return null;
    }

    /**
     * Fold Real binary operations.
     */
    private Expression foldRealBinaryOp(double left, double right, String op, Span span) {
        return switch (op) {
            case "Plus" -> createRealLiteral(left + right, span);
            case "Minus" -> createRealLiteral(left - right, span);
            case "Mult" -> createRealLiteral(left * right, span);
            case "Div" -> right != 0.0 ? createRealLiteral(left / right, span) : null;
            case "Less" -> createBooleanLiteral(left < right, span);
            case "LessEqual" -> createBooleanLiteral(left <= right, span);
            case "Greater" -> createBooleanLiteral(left > right, span);
            case "GreaterEqual" -> createBooleanLiteral(left >= right, span);
            case "Equal" -> createBooleanLiteral(Math.abs(left - right) < 1e-9, span);
            default -> null;
        };
    }

    /**
     * Fold Real unary operations.
     */
    private Expression foldRealUnaryOp(double value, String op, Span span) {
        return switch (op) {
            case "UnaryMinus" -> createRealLiteral(-value, span);
            case "UnaryPlus" -> createRealLiteral(value, span);
            default -> null;
        };
    }

    // ========== HELPER METHODS ==========

    /**
     * Extract integer constant from ConstructorCall(literal).
     */
    private Integer extractIntegerConstant(Expression expr) {
        if (expr instanceof ConstructorCall) {
            ConstructorCall ctor = (ConstructorCall) expr;
            if (ctor.getClassName().equals("Integer") && ctor.getArguments().size() == 1) {
                Expression arg = ctor.getArguments().get(0);
                if (arg instanceof IntegerLiteral) {
                    return ((IntegerLiteral) arg).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Extract boolean constant from ConstructorCall(literal).
     */
    private Boolean extractBooleanConstant(Expression expr) {
        if (expr instanceof ConstructorCall) {
            ConstructorCall ctor = (ConstructorCall) expr;
            if (ctor.getClassName().equals("Boolean") && ctor.getArguments().size() == 1) {
                Expression arg = ctor.getArguments().get(0);
                if (arg instanceof BooleanLiteral) {
                    return ((BooleanLiteral) arg).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Extract real constant from ConstructorCall(literal).
     */
    private Double extractRealConstant(Expression expr) {
        if (expr instanceof ConstructorCall) {
            ConstructorCall ctor = (ConstructorCall) expr;
            if (ctor.getClassName().equals("Real") && ctor.getArguments().size() == 1) {
                Expression arg = ctor.getArguments().get(0);
                if (arg instanceof RealLiteral) {
                    return ((RealLiteral) arg).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Create Integer constructor call with literal.
     * If the value is already wrapped in a ConstructorCall, extract the literal.
     */
    private Expression createIntegerLiteral(int value, Span span) {
        IntegerLiteral lit = new IntegerLiteral(value, span);
        return new ConstructorCall("Integer", List.of(lit), span);
    }

    /**
     * Create Boolean constructor call with literal.
     */
    private Expression createBooleanLiteral(boolean value, Span span) {
        BooleanLiteral lit = new BooleanLiteral(value, span);
        return new ConstructorCall("Boolean", List.of(lit), span);
    }

    /**
     * Create Real constructor call with literal.
     */
    private Expression createRealLiteral(double value, Span span) {
        RealLiteral lit = new RealLiteral(value, span);
        return new ConstructorCall("Real", List.of(lit), span);
    }

    // ========== OTHER EXPRESSION VISITS ==========

    @Override
    public Expression visit(ConstructorCall node) {
        // Fold arguments
        for (int i = 0; i < node.getArguments().size(); i++) {
            Expression arg = node.getArguments().get(i);
            Expression folded = arg.accept(this);
            if (folded != null && folded != arg) {
                node.getArguments().set(i, folded);
                changed = true;
            }
        }

        // Example: Boolean(Boolean(false)) → Boolean(false)
        if (node.getArguments().size() == 1) {
            Expression arg = node.getArguments().get(0);

            // Check if argument is a ConstructorCall of the same type
            if (arg instanceof ConstructorCall) {
                ConstructorCall innerCtor = (ConstructorCall) arg;

                // If inner constructor is same type and has a literal argument
                if (innerCtor.getClassName().equals(node.getClassName()) &&
                        innerCtor.getArguments().size() == 1) {

                    Expression innerArg = innerCtor.getArguments().get(0);

                    // If it's a literal, unwrap to: OuterType(Literal)
                    if (innerArg instanceof IntegerLiteral ||
                            innerArg instanceof BooleanLiteral ||
                            innerArg instanceof RealLiteral) {

                        expressionsFolded++;
                        changed = true;
                        return new ConstructorCall(node.getClassName(),
                                List.of(innerArg),
                                node.getSpan());
                    }
                }
            }
        }

        return node;
    }

    @Override
    public Expression visit(IdentifierExpr node) {
        return node;  // Can't fold variables
    }

    @Override
    public Expression visit(MemberAccess node) {
        return node;  // Can't fold field access
    }

    @Override
    public Expression visit(ThisExpr node) {
        return node;
    }

    @Override
    public Expression visit(BooleanLiteral node) {
        return node;
    }

    @Override
    public Expression visit(IntegerLiteral node) {
        return node;
    }

    @Override
    public Expression visit(RealLiteral node) {
        return node;
    }

    @Override
    public Expression visit(UnknownExpression node) {
        return node;
    }
}
