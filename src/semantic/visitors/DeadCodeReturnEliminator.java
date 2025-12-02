package semantic.visitors;

import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Dead Code Eliminator - removes unreachable statements after return.
 *
 * This is a simple, focused optimization that only handles:
 * - Statements directly after a return statement in the same block
 *
 * Example:
 *   method foo() is
 *     return Integer(5)
 *     var x : Integer(10)  // removed
 *   end
 */
public class DeadCodeReturnEliminator implements ASTVisitor<Void> {

    private int statementsRemoved = 0;

    public DeadCodeReturnEliminator() {
    }

    /**
     * Run optimization on entire program.
     */
    public void optimize(Program program) {
        statementsRemoved = 0;
        program.accept(this);

        if (statementsRemoved > 0) {
            System.out.println("Dead code elimination: removed " + statementsRemoved +
                    " unreachable statement(s)");
        }
    }

    public int getStatementsRemoved() {
        return statementsRemoved;
    }

    // ========== VISITOR METHODS ==========

    @Override
    public Void visit(Program node) {
        for (ClassDecl classDecl : node.getClasses()) {
            classDecl.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ClassDecl node) {
        for (MemberDecl member : node.getMembers()) {
            member.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(MethodDecl node) {
        if (node.getBody().isEmpty() || node.getBody().get().isEmpty()) {
            return null;
        }
        List<Statement> optimizedBody = eliminateDeadCode(node.getBody().get());

        if (optimizedBody.size() < node.getBody().get().size()) {
            int removed = node.getBody().get().size() - optimizedBody.size();
            statementsRemoved += removed;

            node.setBody(optimizedBody);
        }

        return null;
    }

    @Override
    public Void visit(ConstructorDecl node) {
        if (node.getBody() == null || node.getBody().isEmpty()) {
            return null;
        }
        List<Statement> optimizedBody = eliminateDeadCode(node.getBody());
        if (optimizedBody.size() < node.getBody().size()) {
            int removed = node.getBody().size() - optimizedBody.size();
            statementsRemoved += removed;
            node.setBody(optimizedBody);
        }
        return null;
    }

    /**
     * Core optimization: remove statements after return.
     */
    private List<Statement> eliminateDeadCode(List<Statement> statements) {
        List<Statement> result = new ArrayList<>();
        boolean foundReturn = false;
        for (Statement stmt : statements) {
            if (foundReturn) {
                // Skip this statement - it's dead code
                continue;
            }
            result.add(stmt);
            if (stmt instanceof ReturnStatement) {
                foundReturn = true;
                // Everything after this is unreachable
            }
            optimizeNestedBlocks(stmt);
        }
        return result;
    }

    /**
     * Recursively optimize statements within if/while blocks.
     */
    private void optimizeNestedBlocks(Statement stmt) {
        if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt;

            // Optimize then branch
            List<Statement> thenBranch = eliminateDeadCode(ifStmt.getThenBranch());
            if (thenBranch.size() < ifStmt.getThenBranch().size()) {
                statementsRemoved += (ifStmt.getThenBranch().size() - thenBranch.size());
                ifStmt.setThenBranch(thenBranch);
            }

            // Optimize else branch
            if (ifStmt.getElseBranch() != null) {
                List<Statement> elseBranch = eliminateDeadCode(ifStmt.getElseBranch());
                if (elseBranch.size() < ifStmt.getElseBranch().size()) {
                    statementsRemoved += (ifStmt.getElseBranch().size() - elseBranch.size());
                    ifStmt.setElseBranch(elseBranch);
                }
            }
        } else if (stmt instanceof WhileLoop) {
            WhileLoop loop = (WhileLoop) stmt;

            // Optimize loop body
            List<Statement> loopBody = eliminateDeadCode(loop.getBody());
            if (loopBody.size() < loop.getBody().size()) {
                statementsRemoved += (loop.getBody().size() - loopBody.size());
                loop.setBody(loopBody);
            }
        }
    }

    // ========== UNUSED VISITOR METHODS ==========

    @Override
    public Void visit(VariableDecl node) {
        return null;
    }

    @Override
    public Void visit(IdentifierExpr node) {
        return null;
    }

    @Override
    public Void visit(MethodCall node) {
        return null;
    }

    @Override
    public Void visit(ConstructorCall node) {
        return null;
    }

    @Override
    public Void visit(MemberAccess node) {
        return null;
    }

    @Override
    public Void visit(ThisExpr node) {
        return null;
    }

    @Override
    public Void visit(BooleanLiteral node) {
        return null;
    }

    @Override
    public Void visit(IntegerLiteral node) {
        return null;
    }

    @Override
    public Void visit(RealLiteral node) {
        return null;
    }

    @Override
    public Void visit(UnknownExpression node) {
        return null;
    }

    @Override
    public Void visit(Assignment node) {
        return null;
    }

    @Override
    public Void visit(IfStatement node) {
        return null;
    }

    @Override
    public Void visit(WhileLoop node) {
        return null;
    }

    @Override
    public Void visit(ReturnStatement node) {
        return null;
    }

    @Override
    public Void visit(VariableDeclStatement node) {
        return null;
    }

    @Override
    public Void visit(ExpressionStatement node) {
        return null;
    }

    @Override
    public Void visit(UnknownStatement node) {
        return null;
    }
}
