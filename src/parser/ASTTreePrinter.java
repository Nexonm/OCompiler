package parser;

import parser.ast.*;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

/**
 * Prints the AST in a beautiful tree structure with ANSI colors.
 * Uses box-drawing characters for visual representation.
 *
 * Example output (with colors):
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

    // ANSI Color Codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_MAGENTA = "\u001B[35m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private final boolean useColors;

    public ASTTreePrinter() {
        this(true);
    }

    public ASTTreePrinter(boolean useColors) {
        this.output = new StringBuilder();
        this.indent = "";
        this.useColors = useColors;
    }

    /**
     * Prints the entire program AST.
     * @param program The root program node
     * @return Formatted tree string
     */
    public String print(Program program) {
        output = new StringBuilder();
        indent = "";
        printHeader();
        visitProgram(program);
        printEnd();
        return output.toString();
    }

    private void printHeader() {
        String border = "═".repeat(70);
        output.append(colorize(ANSI_CYAN, border)).append("\n");
        output.append(colorize(ANSI_BOLD + ANSI_CYAN, centerText("AST TREE", 70))).append("\n");
        output.append(colorize(ANSI_CYAN, border)).append("\n");
    }

    private void printEnd(){
        String border = "═".repeat(70);
        output.append("\n").append(colorize(ANSI_CYAN, border)).append("\n");
    }

    // ========== PROGRAM & CLASS ==========

    private void visitProgram(Program program) {
        output.append(colorize(ANSI_BOLD + ANSI_CYAN, "Program")).append("\n");

        var classes = program.getClasses();
        for (int i = 0; i < classes.size(); i++) {
            boolean isLast = (i == classes.size() - 1);
            visitClass(classes.get(i), isLast);
        }
    }

    private void visitClass(ClassDecl classDecl, boolean isLast) {
        output.append(indent).append(colorize(ANSI_WHITE, isLast ? LAST_BRANCH : BRANCH));
        output.append(colorize(ANSI_BOLD + ANSI_GREEN, "ClassDecl")).append(": ");
        output.append(colorize(ANSI_CYAN, classDecl.getName()));

        if (classDecl.hasBaseClass()) {
            output.append(" ").append(colorize(ANSI_YELLOW, "extends"))
                    .append(" ").append(colorize(ANSI_CYAN, classDecl.getBaseClassNameOrNull()));
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
        output.append(baseIndent).append(colorize(ANSI_WHITE, isLast ? LAST_BRANCH : BRANCH));

        if (member instanceof VariableDecl var) {
            output.append(colorize(ANSI_BOLD + ANSI_GREEN, "VariableDecl")).append(": ");
            output.append(colorize(ANSI_MAGENTA, var.getName())).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(var.getInitializer(), newIndent, true);

        } else if (member instanceof MethodDecl method) {
            output.append(colorize(ANSI_BOLD + ANSI_GREEN, "MethodDecl")).append(": ");
            output.append(colorize(ANSI_BLUE, method.getName()));
            visitMethodSignature(method);
            output.append("\n");

            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            if (method.hasBody()) {
                visitStatements(method.getBodyOrNull(), newIndent);
            } else {
                output.append(newIndent).append(colorize(ANSI_WHITE, LAST_BRANCH))
                        .append(colorize(ANSI_YELLOW, "[forward declaration]")).append("\n");
            }

        } else if (member instanceof ConstructorDecl ctor) {
            output.append(colorize(ANSI_BOLD + ANSI_GREEN, "ConstructorDecl"));
            visitParameters(ctor.getParameters());
            output.append("\n");

            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitStatements(ctor.getBody(), newIndent);
        }
    }

    private void visitMethodSignature(MethodDecl method) {
        output.append(colorize(ANSI_WHITE, "("));
        var params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(colorize(ANSI_WHITE, ", "));
            Parameter p = params.get(i);
            output.append(colorize(ANSI_MAGENTA, p.getName()))
                    .append(colorize(ANSI_WHITE, ":"))
                    .append(colorize(ANSI_CYAN, p.getTypeName()));
        }
        output.append(colorize(ANSI_WHITE, ")"));

        if (method.hasReturnType()) {
            output.append(" ").append(colorize(ANSI_YELLOW, ":")).append(" ")
                    .append(colorize(ANSI_CYAN, method.getReturnTypeNameOrNull()));
        }
    }

    private void visitParameters(java.util.List<Parameter> params) {
        output.append(colorize(ANSI_WHITE, "("));
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(colorize(ANSI_WHITE, ", "));
            Parameter p = params.get(i);
            output.append(colorize(ANSI_MAGENTA, p.getName()))
                    .append(colorize(ANSI_WHITE, ":"))
                    .append(colorize(ANSI_CYAN, p.getTypeName()));
        }
        output.append(colorize(ANSI_WHITE, ")"));
    }

    // ========== STATEMENTS ==========

    private void visitStatements(java.util.List<Statement> statements, String baseIndent) {
        if (statements.isEmpty()) {
            output.append(baseIndent).append(colorize(ANSI_WHITE, LAST_BRANCH))
                    .append(colorize(ANSI_YELLOW, "[empty body]")).append("\n");
            return;
        }

        for (int i = 0; i < statements.size(); i++) {
            boolean isLast = (i == statements.size() - 1);
            visitStatement(statements.get(i), baseIndent, isLast);
        }
    }

    private void visitStatement(Statement stmt, String baseIndent, boolean isLast) {
        output.append(baseIndent).append(colorize(ANSI_WHITE, isLast ? LAST_BRANCH : BRANCH));

        if (stmt instanceof ReturnStatement ret) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "ReturnStatement"));
            if (ret.isVoidReturn()) {
                output.append(" ").append(colorize(ANSI_YELLOW, "(void)")).append("\n");
            } else {
                output.append("\n");
                String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
                visitExpression(ret.getValueOrNull(), newIndent, true);
            }
        } else if (stmt instanceof Assignment assign) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "Assignment")).append(": ");
            output.append(colorize(ANSI_MAGENTA, assign.getTargetName())).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(assign.getValue(), newIndent, true);
        } else if (stmt instanceof IfStatement ifStmt) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "IfStatement")).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append(colorize(ANSI_WHITE, "├─"))
                    .append(" ").append(colorize(ANSI_YELLOW, "condition")).append(":\n");
            visitExpression(ifStmt.getCondition(), newIndent + VERTICAL, true);
            output.append(newIndent).append(colorize(ANSI_WHITE, "├─"))
                    .append(" ").append(colorize(ANSI_YELLOW, "then")).append("\n");
            visitStatements(ifStmt.getThenBranch(), newIndent + VERTICAL);
            if (ifStmt.hasElseBranch()) {
                output.append(newIndent).append(colorize(ANSI_WHITE, "└─"))
                        .append(" ").append(colorize(ANSI_YELLOW, "else")).append(":\n");
                visitStatements(ifStmt.getElseBranch(), newIndent + SPACE);
            }
        } else if (stmt instanceof WhileLoop whileLoop) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "WhileLoop")).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append(colorize(ANSI_WHITE, "├─"))
                    .append(" ").append(colorize(ANSI_YELLOW, "condition")).append(":\n");
            visitExpression(whileLoop.getCondition(), newIndent + VERTICAL, true);
            output.append(newIndent).append(colorize(ANSI_WHITE, "└─"))
                    .append(" ").append(colorize(ANSI_YELLOW, "body")).append(":\n");
            visitStatements(whileLoop.getBody(), newIndent + SPACE);
        } else if (stmt instanceof VariableDeclStatement varDeclStmt) {
            var varDecl = varDeclStmt.getVariableDecl();
            output.append(colorize(ANSI_BOLD + ANSI_GREEN, "VariableDecl")).append(": ");
            output.append(colorize(ANSI_MAGENTA, varDecl.getName())).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(varDecl.getInitializer(), newIndent, true);
        } else {
            output.append(colorize(ANSI_BOLD + ANSI_RED, stmt.getClass().getSimpleName())).append("\n");
        }
    }

    // ========== EXPRESSIONS ==========

    private void visitExpression(Expression expr, String baseIndent, boolean isLast) {
        output.append(baseIndent).append(colorize(ANSI_WHITE, isLast ? LAST_BRANCH : BRANCH));

        if (expr instanceof IntegerLiteral lit) {
            output.append(colorize(ANSI_BOLD + ANSI_MAGENTA, "IntegerLiteral")).append(": ");
            output.append(colorize(ANSI_GREEN, String.valueOf(lit.getValue()))).append("\n");

        } else if (expr instanceof RealLiteral lit) {
            output.append(colorize(ANSI_BOLD + ANSI_MAGENTA, "RealLiteral")).append(": ");
            output.append(colorize(ANSI_GREEN, String.valueOf(lit.getValue()))).append("\n");

        } else if (expr instanceof BooleanLiteral lit) {
            output.append(colorize(ANSI_BOLD + ANSI_MAGENTA, "BooleanLiteral")).append(": ");
            output.append(colorize(ANSI_GREEN, String.valueOf(lit.getValue()))).append("\n");

        } else if (expr instanceof ThisExpr) {
            output.append(colorize(ANSI_BOLD + ANSI_YELLOW, "ThisExpression")).append("\n");

        } else if (expr instanceof IdentifierExpr id) {
            output.append(colorize(ANSI_BOLD + ANSI_MAGENTA, "IdentifierExpr")).append(": ");
            output.append(colorize(ANSI_CYAN, id.getName())).append("\n");

        } else if (expr instanceof ConstructorCall call) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "ConstructorCall")).append(": ");
            output.append(colorize(ANSI_CYAN, call.getClassName()));
            output.append(colorize(ANSI_WHITE, "("))
                    .append(colorize(ANSI_GREEN, String.valueOf(call.getArgumentCount())))
                    .append(colorize(ANSI_WHITE, " args)")).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            var args = call.getArguments();
            for (int i = 0; i < args.size(); i++) {
                boolean argIsLast = (i == args.size() - 1);
                visitExpression(args.get(i), newIndent, argIsLast);
            }
        } else if (expr instanceof MethodCall call) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "MethodCall")).append(": ");
            output.append(colorize(ANSI_CYAN, call.getMethodName()));
            output.append(colorize(ANSI_WHITE, "("))
                    .append(colorize(ANSI_GREEN, String.valueOf(call.getArgumentCount())))
                    .append(colorize(ANSI_WHITE, " args)")).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            output.append(newIndent).append(colorize(ANSI_WHITE, "├─"))
                    .append(" ").append(colorize(ANSI_YELLOW, "target")).append(":\n");
            visitExpression(call.getTarget(), newIndent + VERTICAL, true);
            if (call.getArgumentCount() > 0) {
                output.append(newIndent).append(colorize(ANSI_WHITE, "└─"))
                        .append(" ").append(colorize(ANSI_YELLOW, "arguments")).append(":\n");
                var args = call.getArguments();
                for (int i = 0; i < args.size(); i++) {
                    visitExpression(args.get(i), newIndent + SPACE, i == args.size() - 1);
                }
            }
        } else if (expr instanceof MemberAccess access) {
            output.append(colorize(ANSI_BOLD + ANSI_BLUE, "MemberAccess")).append(": ");
            output.append(colorize(ANSI_CYAN, access.getMemberName())).append("\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(access.getTarget(), newIndent, true);
        } else {
            output.append(colorize(ANSI_BOLD + ANSI_RED, expr.getClass().getSimpleName())).append("\n");
        }
    }

    // ========== COLOR HELPER ==========

    /**
     * Apply ANSI color codes to text if colors are enabled.
     */
    private String colorize(String colorCode, String text) {
        if (!useColors) {
            return text;
        }
        return colorCode + text + ANSI_RESET;
    }

    /**
     * Prints AST to console.
     * @param program The program to print
     */
    public static void printToConsole(Program program) {
        ASTTreePrinter printer = new ASTTreePrinter();
        System.out.println(printer.print(program));
    }

    /**
     * Prints AST to console without colors.
     * @param program The program to print
     */
    public static void printToConsoleNoColors(Program program) {
        ASTTreePrinter printer = new ASTTreePrinter(false);
        System.out.println(printer.print(program));
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}
