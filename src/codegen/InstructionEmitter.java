package codegen;

/**
 * Builds Jasmin assembly code with proper formatting and indentation.
 *
 * Responsibilities:
 * - Emit class directives (.class, .super, .field)
 * - Emit method headers and footers
 * - Emit instructions with proper indentation
 * - Emit labels without indentation
 * - Add comments for readability
 */
public class InstructionEmitter {

    private final StringBuilder code;
    private int indentLevel;
    private static final String INDENT = "    "; // 4 spaces

    public InstructionEmitter() {
        this.code = new StringBuilder();
        this.indentLevel = 0;
    }

    // ========== BASIC EMISSION ==========

    /**
     * Emit a raw line without indentation.
     * Use for directives like .class, .super, .method, .end method
     */
    public void emitRaw(String line) {
        code.append(line).append("\n");
    }

    /**
     * Emit an instruction with current indentation.
     * Use for instructions like iload, iadd, return
     */
    public void emit(String instruction) {
        indent();
        code.append(instruction).append("\n");
    }

    /**
     * Emit a label (no indentation, ends with colon).
     * Use for control flow labels
     */
    public void emitLabel(String label) {
        // Labels must be at column 0 in Jasmin
        code.append(label).append(":\n");
    }

    /**
     * Emit a comment (with indentation).
     */
    public void emitComment(String comment) {
        indent();
        code.append("; ").append(comment).append("\n");
    }

    /**
     * Emit a blank line for readability.
     */
    public void emitBlank() {
        code.append("\n");
    }

    // ========== CLASS STRUCTURE ==========

    /**
     * Emit class header with optional parent class.
     *
     * @param className The class name
     * @param parentClassName The parent class name (null for Object)
     */
    public void emitClassHeader(String className, String parentClassName) {
        emitRaw(".class public " + className);

        // If no parent specified, default to java/lang/Object
        if (parentClassName == null || parentClassName.isEmpty()) {
            emitRaw(".super java/lang/Object");
        } else {
            emitRaw(".super " + parentClassName);
        }

        emitBlank();
    }

    /**
     * Emit class header with default parent (java/lang/Object).
     *
     * @param className The class name
     */
    public void emitClassHeader(String className) {
        emitClassHeader(className, null);
    }


    /**
     * Emit field declaration.
     *
     * @param name Field name
     * @param descriptor Field type descriptor (I, Z, D, LClassName;)
     */
    public void emitField(String name, String descriptor) {
        emitRaw(".field private " + name + " " + descriptor);
    }

    // ========== CONSTRUCTOR HELPERS ==========

    /**
     * Emit constructor header (just a wrapper for clarity).
     * Constructor name is always <init>, return type is always V.
     *
     * @param paramDescriptors Parameter descriptors (e.g., "" for no params, "I" for one int)
     */
    public void emitConstructorHeader(String paramDescriptors) {
        String descriptor = "(" + paramDescriptors + ")V";
        emitMethodHeader("<init>", descriptor);
    }

    /**
     * Emit parameterless constructor header.
     * Convenience method for the common case: this()
     */
    public void emitDefaultConstructorHeader() {
        emitConstructorHeader("");
    }

    /**
     * Emit super constructor call (must be first in constructor).
     *
     * @param parentClassName Parent class name
     * @param paramDescriptors Parameter descriptors for parent constructor
     */
    public void emitSuperCall(String parentClassName, String paramDescriptors) {
        emit("aload_0");  // Load 'this'
        String descriptor = "(" + paramDescriptors + ")V";
        emitInvoke(parentClassName, "<init>", descriptor, "special");
    }

    /**
     * Emit super constructor call with no parameters.
     * Most common case: calling parent's default constructor.
     */
    public void emitSuperCall(String parentClassName) {
        emitSuperCall(parentClassName, "");
    }


    // ========== METHOD STRUCTURE ==========

    /**
     * Emit method header.
     *
     * @param name Method name (or <init> for constructor)
     * @param descriptor Method descriptor (e.g., "()V" or "(I)I")
     */
    public void emitMethodHeader(String name, String descriptor) {
        emitMethodHeader(name, descriptor, false);
    }

    /**
     * Emit method header with optional static modifier.
     *
     * @param name Method name (or <init> for constructor)
     * @param descriptor Method descriptor (e.g., "()V" or "(I)I")
     * @param isStatic true to emit a static method
     */
    public void emitMethodHeader(String name, String descriptor, boolean isStatic) {
        emitBlank();
        StringBuilder builder = new StringBuilder(".method public ");
        if (isStatic) {
            builder.append("static ");
        }
        builder.append(name).append(descriptor);
        emitRaw(builder.toString());
        increaseIndent(); // Indent everything inside method
    }

    /**
     * Emit method limits (stack and locals).
     *
     * @param stackSize Maximum stack depth
     * @param localsSize Number of local variable slots
     */
    public void emitLimits(int stackSize, int localsSize) {
        emit(".limit stack " + stackSize);
        emit(".limit locals " + localsSize);
        emitBlank();
    }

    /**
     * Emit method footer.
     */
    public void emitMethodFooter() {
        decreaseIndent(); // Back to class level
        emitRaw(".end method");
    }

    /**
     * Emit integer comparison branch.
     * @param comparison One of: "eq", "ne", "lt", "le", "gt", "ge"
     * @param label Label to jump to if comparison is true
     */
    public void emitIfICmp(String comparison, String label) {
        emit("if_icmp" + comparison + " " + label);
    }

    /**
     * Emit conditional branch (single value compared to 0).
     * @param comparison One of: "eq", "ne", "lt", "le", "gt", "ge"
     * @param label Label to jump to if comparison is true
     */
    public void emitIf(String comparison, String label) {
        emit("if" + comparison + " " + label);
    }

    /**
     * Emit unconditional jump.
     */
    public void emitGoto(String label) {
        emit("goto " + label);
    }


    // ========== INDENTATION CONTROL ==========

    /**
     * Increase indentation level.
     */
    public void increaseIndent() {
        indentLevel++;
    }

    /**
     * Decrease indentation level.
     */
    public void decreaseIndent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    /**
     * Add current indentation to code.
     */
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            code.append(INDENT);
        }
    }

    // ========== COMMON INSTRUCTION PATTERNS ==========

    /**
     * Emit a load instruction for local variable.
     *
     * @param slot Local variable slot number
     * @param type Type: 'i' (int), 'a' (reference), 'd' (double)
     */
    public void emitLoad(int slot, char type) {
        emit(type + "load " + slot);
    }

    /**
     * Emit a store instruction for local variable.
     *
     * @param slot Local variable slot number
     * @param type Type: 'i' (int), 'a' (reference), 'd' (double)
     */
    public void emitStore(int slot, char type) {
        emit(type + "store " + slot);
    }

    /**
     * Emit a push constant instruction.
     * Chooses the most efficient instruction based on value.
     */
    public void emitPushInt(int value) {
        if (value >= -1 && value <= 5) {
            // Use iconst_N for -1 to 5
            emit("iconst_" + value);
        } else if (value >= -128 && value <= 127) {
            // Use bipush for byte range
            emit("bipush " + value);
        } else if (value >= -32768 && value <= 32767) {
            // Use sipush for short range
            emit("sipush " + value);
        } else {
            // Use ldc for larger values
            emit("ldc " + value);
        }
    }

    /**
     * Emit a push double constant instruction.
     * Chooses the most efficient instruction based on value.
     */
    public void emitPushDouble(double value) {
        if (value == 0.0) {
            emit("dconst_0");
        } else if (value == 1.0) {
            emit("dconst_1");
        } else {
            emit("ldc2_w " + value);  // ldc2_w for double constants
        }
    }

    /**
     * Emit object creation sequence.
     * Pattern: new ClassName + dup
     * Use before calling constructor with invokespecial
     */
    public void emitNew(String className) {
        emit("new " + className);
        emit("dup");  // Duplicate reference for constructor call
    }


    /**
     * Emit a push boolean constant instruction.
     * Booleans are represented as int: 0 = false, 1 = true
     */
    public void emitPushBoolean(boolean value) {
        if (value) {
            emit("iconst_1");  // true = 1
        } else {
            emit("iconst_0");  // false = 0
        }
    }

    /**
     * Emit arithmetic operation.
     * @param op Operation: "add", "sub", "mul", "div", "rem", "neg"
     * @param type Type: 'i' (int) or 'd' (double)
     */
    public void emitArithmetic(String op, char type) {
        emit(type + op);
    }

    // Convenience methods
    public void emitAdd(char type) { emitArithmetic("add", type); }
    public void emitSub(char type) { emitArithmetic("sub", type); }
    public void emitMul(char type) { emitArithmetic("mul", type); }
    public void emitDiv(char type) { emitArithmetic("div", type); }
    public void emitRem(char type) { emitArithmetic("rem", type); }
    public void emitNeg(char type) { emitArithmetic("neg", type); }



    /**
     * Emit return instruction based on type.
     *
     * @param type Return type: 'i' (int), 'a' (reference), 'd' (double), 'v' (void)
     */
    public void emitReturn(char type) {
        if (type == 'v') {
            emit("return");
        } else {
            emit(type + "return");
        }
    }

    /**
     * Emit field access instruction.
     *
     * @param className Class containing the field
     * @param fieldName Field name
     * @param descriptor Field type descriptor
     * @param isGet true for getfield, false for putfield
     */
    public void emitFieldAccess(String className, String fieldName,
                                String descriptor, boolean isGet) {
        String instruction = isGet ? "getfield" : "putfield";
        emit(instruction + " " + className + "/" + fieldName + " " + descriptor);
    }

    /**
     * Emit method invocation.
     *
     * @param className Class containing the method
     * @param methodName Method name
     * @param descriptor Method descriptor
     * @param invokeType Type: "virtual", "special", "static"
     */
    public void emitInvoke(String className, String methodName,
                           String descriptor, String invokeType) {
        emit("invoke" + invokeType + " " + className + "/" + methodName + descriptor);
    }

    // ========== OUTPUT ==========

    /**
     * Get the complete generated Jasmin code.
     *
     * @return Complete Jasmin assembly code as string
     */
    public String getCode() {
        return code.toString();
    }

    /**
     * Clear all generated code (useful for testing).
     */
    public void clear() {
        code.setLength(0);
        indentLevel = 0;
    }

    /**
     * Get current code length (for debugging).
     */
    public int getLength() {
        return code.length();
    }

    @Override
    public String toString() {
        return getCode();
    }
}
