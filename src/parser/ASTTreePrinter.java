package parser;

import parser.ast.*;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    // ANSI escape codes for red text
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

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

    public String printErrors(Parser parser) {
        StringBuilder errorOutput = new StringBuilder();

        // ANSI escape codes for red text
        String RED = "\u001B[31m";
        String RESET = "\u001B[0m";

        List<String> errors = parser.getErrors();

        // If there are more than 5 errors, likely a cascading error situation
        if (errors.size() > 5) {
            errorOutput.append(RED)
                    .append("=== CASCADING ERRORS DETECTED ===\n")
                    .append(RESET);

            // Find and display the first error (root cause)
            if (!errors.isEmpty()) {
                errorOutput.append(RED)
                        .append("Primary error (likely root cause):\n")
                        .append("  ")
                        .append(errors.get(0))
                        .append("\n\n")
                        .append(RESET);
            }

            // Group subsequent errors by line number
            Map<Integer, List<String>> errorsByLine = groupErrorsByLine(errors);

            errorOutput.append(RED)
                    .append("Affected lines: ");

            Set<Integer> lines = errorsByLine.keySet();
            errorOutput.append(lines.stream()
                            .sorted()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")))
                    .append("\n")
                    .append(RESET);

            errorOutput.append(RED)
                    .append("\nTotal errors: ")
                    .append(errors.size())
                    .append(" (")
                    .append(errors.size() - 1)
                    .append(" cascading errors suppressed)\n")
                    .append(RESET);

            errorOutput.append(RED)
                    .append("\nSuggestion: Fix the first error and re-compile. ")
                    .append("Subsequent errors may disappear.\n")
                    .append(RESET);
        } else {
            // For 5 or fewer errors, display all normally
            for (String message : errors) {
                errorOutput.append(RED)
                        .append(message)
                        .append(RESET)
                        .append("\n");
            }
        }

        return errorOutput.toString();
    }

    /**
     * Helper method to group errors by line number.
     * Extracts line numbers from error messages.
     */
    private Map<Integer, List<String>> groupErrorsByLine(List<String> errors) {
        Map<Integer, List<String>> errorsByLine = new TreeMap<>();

        for (String error : errors) {
            // Extract line number from error message (e.g., "Parse error at line 8")
            int lineNumber = extractLineNumber(error);

            errorsByLine.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(error);
        }

        return errorsByLine;
    }

    /**
     * Extracts line number from error message.
     * Returns -1 if no line number found.
     */
    private int extractLineNumber(String errorMessage) {
        // Pattern: "line X" where X is a number
        Pattern pattern = Pattern.compile("line\\s+(\\d+)");
        Matcher matcher = pattern.matcher(errorMessage);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
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
        } else if (stmt instanceof ExpressionStatement exprStmt) {
            output.append("ExpressionStatement\n");
            String newIndent = baseIndent + (isLast ? SPACE : VERTICAL);
            visitExpression(exprStmt.getExpression(), newIndent, true);
        } else if (stmt instanceof UnknownStatement unknownStmt){
            output.append(unknownStmt.toString()).append("\n");
        } else {
            output.append("ERROR! " + stmt.getClass().getSimpleName()).append("\n");
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

