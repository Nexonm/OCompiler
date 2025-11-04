package semantic;

import semantic.symbols.*;

import java.util.*;

/**
 * Pretty-prints the symbol table in a tree-like, human-readable format.
 * Displays classes, their fields, methods, and parameters in a visual hierarchy.
 */
public class SymbolTablePrinter {
    private static final String TREE_BRANCH = "├── ";
    private static final String TREE_LAST = "└── ";
    private static final String TREE_VERTICAL = "│   ";
    private static final String TREE_SPACE = "    ";

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_MAGENTA = "\u001B[35m";

    private final boolean useColors;

    public SymbolTablePrinter() {
        this(true); // Use colors by default
    }

    public SymbolTablePrinter(boolean useColors) {
        this.useColors = useColors;
    }

    /**
     * Print the entire symbol table.
     */
    public void print(SymbolTable symbolTable) {
        printHeader();

        // Get global scope and iterate through all classes
        Scope globalScope = symbolTable.getCurrentScope();
        while (globalScope.getParent() != null) {
            globalScope = globalScope.getParent(); // Navigate to root
        }

        printGlobalScope(globalScope);
        printFooter();
    }

    /**
     * Print just the classes (useful after building symbol table).
     */
    public void printClasses(SymbolTable symbolTable) {
        printHeader();

        Scope globalScope = symbolTable.getCurrentScope();
        while (globalScope.getParent() != null) {
            globalScope = globalScope.getParent();
        }

        printGlobalScope(globalScope);
        printFooter();
    }

    private void printHeader() {
        String border = "═".repeat(70);
        println(color(ANSI_CYAN, border));
        println(color(ANSI_BOLD + ANSI_CYAN, centerText("SYMBOL TABLE", 70)));
        println(color(ANSI_CYAN, border));
        println();
    }

    private void printFooter() {
        println();
        String border = "═".repeat(70);
        println(color(ANSI_CYAN, border));
    }

    private void printGlobalScope(Scope globalScope) {
        println(color(ANSI_BOLD + ANSI_BLUE, "Global Scope"));
        println();

        // Collect all class symbols using reflection on Scope
        try {
            java.lang.reflect.Field symbolsField = Scope.class.getDeclaredField("symbols");
            symbolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Symbol> symbols = (Map<String, Symbol>) symbolsField.get(globalScope);

            if (symbols.isEmpty()) {
                println("  " + color(ANSI_YELLOW, "(empty)"));
                return;
            }

            int classCount = 0;
            int totalClasses = (int) symbols.values().stream()
                    .filter(s -> s instanceof ClassSymbol)
                    .count();

            for (Symbol symbol : symbols.values()) {
                if (symbol instanceof ClassSymbol) {
                    classCount++;
                    boolean isLast = (classCount == totalClasses);
                    printClassSymbol((ClassSymbol) symbol, "", isLast);
                }
            }
        } catch (Exception e) {
            println("  " + color(ANSI_YELLOW, "(unable to access symbols)"));
        }
    }

    private void printClassSymbol(ClassSymbol classSymbol, String indent, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;
        String childIndent = indent + (isLast ? TREE_SPACE : TREE_VERTICAL);

        // Class header
        String className = color(ANSI_BOLD + ANSI_GREEN, "class " + classSymbol.getName());
        String parent = classSymbol.getParentClassName()
                .map(p -> color(ANSI_YELLOW, " extends " + p))
                .orElse("");

        println(indent + connector + className + parent);

        // Get fields and methods
        Map<String, VariableSymbol> fields = classSymbol.getAllFields();
        Map<String, MethodSymbol> methods = classSymbol.getAllMethods();

        int totalMembers = fields.size() + methods.size();
        if (totalMembers == 0) {
            println(childIndent + color(ANSI_YELLOW, "(empty class)"));
            return;
        }

        int memberCount = 0;

        // Print fields
        for (VariableSymbol field : fields.values()) {
            memberCount++;
            boolean isLastMember = (memberCount == totalMembers);
            printVariableSymbol(field, childIndent, isLastMember, "field");
        }

        // Print methods (with overload handling)
        Set<String> printedMethods = new HashSet<>();
        for (MethodSymbol method : methods.values()) {
            memberCount++;
            boolean isLastMember = (memberCount == totalMembers);

            // Group overloaded methods
            String methodName = method.getName();
            if (!printedMethods.contains(methodName)) {
                // First time seeing this method name - get all overloads
                List<MethodSymbol> overloads = classSymbol.getMethodsByName(methodName);

                if (overloads.size() == 1) {
                    // No overloading
                    printMethodSymbol(overloads.get(0), childIndent, isLastMember);
                } else {
                    // Multiple overloads - print with group header
                    String connector2 = isLastMember ? TREE_LAST : TREE_BRANCH;
                    String groupIndent = childIndent + (isLastMember ? TREE_SPACE : TREE_VERTICAL);

                    println(childIndent + connector2 + color(ANSI_BOLD + ANSI_BLUE, methodName)
                            + color(ANSI_YELLOW, " (overloaded: " + overloads.size() + " variants)"));

                    // Print each overload variant
                    for (int i = 0; i < overloads.size(); i++) {
                        boolean isLastOverload = (i == overloads.size() - 1);
                        printMethodOverload(overloads.get(i), groupIndent, isLastOverload);
                    }
                }

                printedMethods.add(methodName);
            }
        }
    }

    private void printMethodOverload(MethodSymbol method, String indent, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;

        // Build signature
        StringBuilder params = new StringBuilder("(");
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            if (i > 0) params.append(", ");
            params.append(color(ANSI_CYAN, method.getParameterTypes().get(i)));
        }
        params.append(")");

        // Return type
        String returnType = "";
        if (!method.isConstructor() && method.hasReturnType()) {
            returnType = color(ANSI_GREEN, " : " + method.getReturnType());
        } else if (!method.isConstructor()) {
            returnType = color(ANSI_YELLOW, " : void");
        }
        println(indent + connector + "  " + params + returnType);
    }


    private void printVariableSymbol(VariableSymbol variable, String indent, boolean isLast, String kind) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;

        String varName = color(ANSI_MAGENTA, variable.getName());
        String varType = color(ANSI_CYAN, ": " + variable.getType());
        String varKind = color(ANSI_YELLOW, " [" + kind + "]");

        println(indent + connector + " " + varName + varType + varKind);
    }

    private void printMethodSymbol(MethodSymbol method, String indent, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;
        String childIndent = indent + (isLast ? TREE_SPACE : TREE_VERTICAL);

        // Method header
        String methodName = color(ANSI_BOLD + ANSI_BLUE, method.getName());

        // Parameters
        StringBuilder params = new StringBuilder("(");
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            if (i > 0) params.append(", ");
            params.append(color(ANSI_CYAN, method.getParameterTypes().get(i)));
        }
        params.append(")");

        // Return type
        String returnType = "";
        if (!method.isConstructor() && method.hasReturnType()) {
            returnType = color(ANSI_GREEN, " : " + method.getReturnType());
        } else if (!method.isConstructor()) {
            returnType = color(ANSI_YELLOW, " : void");
        }

        println(indent + connector + "  " + methodName + params + returnType);

        // Show parameter details if any
        if (!method.getParameterTypes().isEmpty()) {
            for (int i = 0; i < method.getParameterTypes().size(); i++) {
                boolean isLastParam = (i == method.getParameterTypes().size() - 1);
                String paramConnector = isLastParam ? TREE_LAST : TREE_BRANCH;
                String paramType = method.getParameterTypes().get(i);
                String paramDisplay = color(ANSI_CYAN, "param" + i) +
                        color(ANSI_YELLOW, " : " + paramType);
                println(childIndent + paramConnector + " " + paramDisplay);
            }
        }
    }

    private String color(String code, String text) {
        if (!useColors) {
            return text;
        }
        return code + text + ANSI_RESET;
    }

    private void println(String text) {
        System.out.println(text);
    }

    private void println() {
        System.out.println();
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}

