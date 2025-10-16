package parser;

import parser.ast.*;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

/**
 * Prints the AST in a beautiful tree structure.
 * Uses box-drawing characters for visual representation.
 *
 * Example output:
 * Program
 * └─ ClassDecl: Calculator
 *    ├─ VariableDecl: result
 *    │  └─ ConstructorCall: Integer(0)
 *    └─ MethodDecl: add(a:Integer, b:Integer) : Integer
 *       └─ ReturnStatement
 *          └─ IntegerLiteral: 42
 */
public class ASTTreePrinter {
    private StringBuilder output;
    private String indent;
    private static final String BRANCH = "├─ ";
    private static final String LAST_BRANCH = "└─ ";
    private static final String VERTICAL = "│  ";
    private static final String SPACE = "   ";

    public ASTTreePrinter() {
        this.output = new StringBuilder();
        this.indent = "";
    }

    /**
     * Prints the entire program AST.
     * @param program The root program node
     * @return Formatted tree string
     */
    public String print(Program program) {
        output = new StringBuilder();
        indent = "";
        visitProgram(program);
        return output.toString();
    }

    // ========== PROGRAM & CLASS ==========

    private void visitProgram(Program program) {
        output.append("Program\n");

        var classes = program.getClasses();
        for (int i = 0; i < classes.size(); i++) {
            boolean isLast = (i == classes.size() - 1);
            visitClass(classes.get(i), isLast);
        }
    }

    private void visitClass(ClassDecl classDecl, boolean isLast) {
        output.append(indent).append(isLast ? LAST_BRANCH : BRANCH);
        output.append("ClassDecl: ").append(classDecl.getName());

        if (classDecl.hasBaseClass()) {
            output.append(" extends ").append(classDecl.getBaseClassNameOrNull());
        }
        output.append("\n");

        String newIndent = indent + (isLast ? SPACE : VERTICAL);
        visitMembers(classDecl.getMembers(), newIndent);
    }

    // ========== MEMBERS ==========

    private void visitMembers(java.util.List<MemberDecl> members, String baseIndent) {
        for (int i = 0; i < members.size(); i++) {
            boolean isLast = (i == members.size() - 1);
            visitMember(members.get(i), baseIndent, isLast);
        }
    }

    private void visitMember(MemberDecl member, String baseIndent, boolean isLast) {
        output.append(baseIndent).append(isLast ? LAST_BRANCH : BRANCH);

        if (member instanceof VariableDecl var) {
            output.append("VariableDecl: ").append(var.getName()).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(var.getInitializer(), newIndent, true);

        } else if (member instanceof MethodDecl method) {
            output.append("MethodDecl: ").append(method.getName());
            visitMethodSignature(method);
            output.append("\n");

            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            if (method.hasBody()) {
                visitStatements(method.getBodyOrNull(), newIndent);
            } else {
                output.append(newIndent).append(LAST_BRANCH)
                        .append("[forward declaration]\n");
            }

        } else if (member instanceof ConstructorDecl ctor) {
            output.append("ConstructorDecl");
            visitParameters(ctor.getParameters());
            output.append("\n");

            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitStatements(ctor.getBody(), newIndent);
        }
    }

    private void visitMethodSignature(MethodDecl method) {
        output.append("(");
        var params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            Parameter p = params.get(i);
            output.append(p.getName()).append(":").append(p.getTypeName());
        }
        output.append(")");

        if (method.hasReturnType()) {
            output.append(" : ").append(method.getReturnTypeNameOrNull());
        }
    }

    private void visitParameters(java.util.List<Parameter> params) {
        output.append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            Parameter p = params.get(i);
            output.append(p.getName()).append(":").append(p.getTypeName());
        }
        output.append(")");
    }

    // ========== STATEMENTS ==========

    private void visitStatements(java.util.List<Statement> statements, String baseIndent) {
        if (statements.isEmpty()) {
            output.append(baseIndent).append(LAST_BRANCH).append("[empty body]\n");
            return;
        }

        for (int i = 0; i < statements.size(); i++) {
            boolean isLast = (i == statements.size() - 1);
            visitStatement(statements.get(i), baseIndent, isLast);
        }
    }

    private void visitStatement(Statement stmt, String baseIndent, boolean isLast) {
        output.append(baseIndent).append(isLast ? LAST_BRANCH : BRANCH);

        if (stmt instanceof ReturnStatement ret) {
            output.append("ReturnStatement");
            if (ret.isVoidReturn()) {
                output.append(" (void)\n");
            } else {
                output.append("\n");
                String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
                visitExpression(ret.getValueOrNull(), newIndent, true);
            }
        } else if (stmt instanceof Assignment assign) {
            output.append("Assignment: ").append(assign.getTargetName()).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(assign.getValue(), newIndent, true);
        } else if (stmt instanceof IfStatement ifStmt) {
            output.append("IfStatement\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append("├─ condition:\n");
            visitExpression(ifStmt.getCondition(), newIndent + VERTICAL, true);
            output.append(newIndent).append("├─ then\n");
            visitStatements(ifStmt.getThenBranch(), newIndent + VERTICAL);
            if (ifStmt.hasElseBranch()) {
                output.append(newIndent).append("└─ else:\n");
                visitStatements(ifStmt.getElseBranch(), newIndent + SPACE);
            }
        } else if (stmt instanceof WhileLoop whileLoop) {
            output.append("WhileLoop\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append("├─ condition:\n");
            visitExpression(whileLoop.getCondition(), newIndent + VERTICAL, true);
            output.append(newIndent).append("└─ body:\n");
            visitStatements(whileLoop.getBody(), newIndent + SPACE);
        } else if (stmt instanceof VariableDeclStatement varDeclStmt) {
            var varDecl = varDeclStmt.getVariableDecl();
            output.append("VariableDecl: ").append(varDecl.getName()).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(varDecl.getInitializer(), newIndent, true);
        } else {
            output.append(stmt.getClass().getSimpleName()).append("\n");
        }
    }

    // ========== EXPRESSIONS ==========

    private void visitExpression(Expression expr, String baseIndent, boolean isLast) {
        output.append(baseIndent).append(isLast ? LAST_BRANCH : BRANCH);

        if (expr instanceof IntegerLiteral lit) {
            output.append("IntegerLiteral: ").append(lit.getValue()).append("\n");

        } else if (expr instanceof RealLiteral lit) {
            output.append("RealLiteral: ").append(lit.getValue()).append("\n");

        } else if (expr instanceof BooleanLiteral lit) {
            output.append("BooleanLiteral: ").append(lit.getValue()).append("\n");

        } else if (expr instanceof ThisExpr) {
            output.append("ThisExpression\n");

        } else if (expr instanceof IdentifierExpr id) {
            output.append("IdentifierExpr: ").append(id.getName()).append("\n");

        } else if (expr instanceof ConstructorCall call) {
            output.append("ConstructorCall: ").append(call.getClassName());
            output.append("(").append(call.getArgumentCount()).append(" args)\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            var args = call.getArguments();
            for (int i = 0; i < args.size(); i++) {
                boolean argIsLast = (i == args.size() - 1);
                visitExpression(args.get(i), newIndent, argIsLast);
            }
        } else if (expr instanceof MethodCall call) {
            output.append("MethodCall: ").append(call.getMethodName());
            output.append("(").append(call.getArgumentCount()).append(" args)\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append("├─ target:\n");
            visitExpression(call.getTarget(), newIndent + VERTICAL, true);
            if (call.getArgumentCount() > 0) {
                output.append(newIndent).append("└─ arguments:\n");
                var args = call.getArguments();
                for (int i = 0; i < args.size(); i++) {
                    visitExpression(args.get(i), newIndent + SPACE, i == args.size() - 1);
                }
            }
        } else if (expr instanceof MemberAccess access) {
            output.append("MemberAccess: ").append(access.getMemberName()).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(access.getTarget(), newIndent, true);
        } else {
            output.append(expr.getClass().getSimpleName()).append("\n");
        }

    }

    /**
     * Prints AST to console.
     * @param program The program to print
     */
    public static void printToConsole(Program program) {
        ASTTreePrinter printer = new ASTTreePrinter();
        System.out.println(printer.print(program));
    }
}

