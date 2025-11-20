package codegen;

import parser.ast.declarations.ClassDecl;

/**
 * Tracks state while generating code for a single method.
 *
 * Bundles together:
 * - Local variable allocation
 * - Label generation
 * - Stack depth tracking
 * - Method metadata
 *
 * This simplifies code generation by keeping all method-specific
 * state in one place.
 */
public class MethodContext {

    private final LocalVariableAllocator locals;
    private final LabelGenerator labelGen;
    private final ClassDecl classDecl;
    private final String methodName;
    private final boolean isStatic;

    private int currentStackDepth;
    private int maxStackDepth;

    /**
     * Creates a method context.
     *
     * @param className The class containing this method
     * @param methodName The method name
     * @param isStatic true if method is static
     */
    public MethodContext(ClassDecl classDecl, String methodName, boolean isStatic) {
        this.classDecl = classDecl;
        this.methodName = methodName;
        this.isStatic = isStatic;

        this.locals = new LocalVariableAllocator(isStatic);
        this.labelGen = new LabelGenerator(classDecl.getName(), methodName);

        this.currentStackDepth = 0;
        this.maxStackDepth = 0;
    }

    // ========== STACK DEPTH TRACKING ==========

    /**
     * Push value(s) onto the operand stack.
     * Updates current and maximum stack depth.
     *
     * @param count Number of stack slots pushed (1 for most, 2 for double)
     */
    public void pushStack(int count) {
        currentStackDepth += count;
        if (currentStackDepth > maxStackDepth) {
            maxStackDepth = currentStackDepth;
        }
    }

    /**
     * Push single value onto stack (convenience method).
     */
    public void pushStack() {
        pushStack(1);
    }

    /**
     * Pop value(s) from the operand stack.
     *
     * @param count Number of stack slots popped
     */
    public void popStack(int count) {
        currentStackDepth -= count;
        if (currentStackDepth < 0) {
            throw new IllegalStateException(
                    "Stack underflow in " + classDecl.getName() + "." + methodName +
                            ": depth would be " + currentStackDepth
            );
        }
    }

    /**
     * Pop single value from stack (convenience method).
     */
    public void popStack() {
        popStack(1);
    }

    /**
     * Get current stack depth.
     *
     * @return Current depth
     */
    public int getCurrentStackDepth() {
        return currentStackDepth;
    }

    /**
     * Get maximum stack depth reached.
     * This is used for .limit stack directive.
     *
     * @return Maximum depth
     */
    public int getMaxStackDepth() {
        return maxStackDepth;
    }

    /**
     * Manually set max stack depth (for edge cases).
     * Use sparingly - prefer pushStack/popStack.
     *
     * @param depth New max depth
     */
    public void setMaxStackDepth(int depth) {
        if (depth > maxStackDepth) {
            maxStackDepth = depth;
        }
    }

    /**
     * Reset stack depth to 0 (for testing/debugging).
     */
    public void resetStackDepth() {
        currentStackDepth = 0;
    }

    // ========== LOCAL VARIABLE MANAGEMENT ==========

    /**
     * Add a parameter to local variables.
     *
     * @param name Parameter name
     * @param isWide true if double-width (Real/double)
     * @return Allocated slot number
     */
    public int addParameter(String name, boolean isWide) {
        return locals.addParameter(name, isWide);
    }

    /**
     * Add a parameter (single-width).
     *
     * @param name Parameter name
     * @return Allocated slot number
     */
    public int addParameter(String name) {
        return locals.addParameter(name);
    }

    /**
     * Allocate a local variable slot.
     *
     * @param name Variable name
     * @param isWide true if double-width (Real/double)
     * @return Allocated slot number
     */
    public int allocateLocal(String name, boolean isWide) {
        return locals.allocate(name, isWide);
    }

    /**
     * Allocate a local variable slot (single-width).
     *
     * @param name Variable name
     * @return Allocated slot number
     */
    public int allocateLocal(String name) {
        return locals.allocate(name);
    }

    /**
     * Get slot number for a variable.
     *
     * @param name Variable name
     * @return Slot number, or -1 if not found
     */
    public int getSlot(String name) {
        return locals.getSlot(name);
    }

    /**
     * Check if variable has been allocated.
     *
     * @param name Variable name
     * @return true if allocated
     */
    public boolean hasVariable(String name) {
        return locals.hasVariable(name);
    }

    /**
     * Get slot for 'this'.
     *
     * @return 0 for instance methods, -1 for static
     */
    public int getThisSlot() {
        return locals.getThisSlot();
    }

    /**
     * Get total number of local variable slots.
     * This is used for .limit locals directive.
     *
     * @return Maximum locals count
     */
    public int getMaxLocals() {
        return locals.getMaxLocals();
    }

    // ========== LABEL GENERATION ==========

    /**
     * Generate next unique label with custom type.
     *
     * @param type Label type (e.g., "IfThen", "WhileStart")
     * @return Unique label string
     */
    public String nextLabel(String type) {
        return labelGen.next(type);
    }

    /**
     * Generate label for if-then branch.
     *
     * @return Label string
     */
    public String ifThen() {
        return labelGen.ifThen();
    }

    /**
     * Generate label for if-else branch.
     *
     * @return Label string
     */
    public String ifElse() {
        return labelGen.ifElse();
    }

    /**
     * Generate label for end of if statement.
     *
     * @return Label string
     */
    public String ifEnd() {
        return labelGen.ifEnd();
    }

    /**
     * Generate label for while loop start.
     *
     * @return Label string
     */
    public String whileStart() {
        return labelGen.whileStart();
    }

    /**
     * Generate label for while loop end.
     *
     * @return Label string
     */
    public String whileEnd() {
        return labelGen.whileEnd();
    }

    /**
     * Generate generic label.
     *
     * @return Label string
     */
    public String genericLabel() {
        return labelGen.generic();
    }

    // ========== METHOD METADATA ==========

    /**
     * Get the class name for this method.
     *
     * @return Class name
     */
    public String getClassName() {
        return classDecl.getName();
    }


    public ClassDecl getClassDecl() {
        return classDecl;
    }

    /**
     * Get the method name.
     *
     * @return Method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Check if this is a static method.
     *
     * @return true if static
     */
    public boolean isStatic() {
        return isStatic;
    }

    // ========== CONVENIENCE METHODS ==========

    /**
     * Record a binary operation on the stack.
     * Pops 2, pushes 1 result.
     *
     * Example: iadd, imul, etc.
     */
    public void recordBinaryOp() {
        popStack(2);
        pushStack(1);
    }

    /**
     * Record a unary operation on the stack.
     * Pops 1, pushes 1 result.
     *
     * Example: ineg, i2d, etc.
     */
    public void recordUnaryOp() {
        popStack(1);
        pushStack(1);
    }

    /**
     * Record a comparison operation.
     * Pops 2, pushes nothing (result is branch).
     *
     * Example: if_icmpgt
     */
    public void recordComparison() {
        popStack(2);
    }

    /**
     * Record pushing a constant.
     *
     * @param isWide true for double (pushes 2 slots)
     */
    public void recordPushConstant(boolean isWide) {
        pushStack(isWide ? 2 : 1);
    }

    /**
     * Record loading a local variable.
     *
     * @param isWide true for double (pushes 2 slots)
     */
    public void recordLoad(boolean isWide) {
        pushStack(isWide ? 2 : 1);
    }

    /**
     * Record storing to a local variable.
     *
     * @param isWide true for double (pops 2 slots)
     */
    public void recordStore(boolean isWide) {
        popStack(isWide ? 2 : 1);
    }

    /**
     * Record method invocation.
     *
     * @param numArgs Number of arguments (popped from stack)
     * @param hasReturn true if method returns a value
     * @param returnIsWide true if return value is double
     */
    public void recordMethodCall(int numArgs, boolean hasReturn, boolean returnIsWide) {
        // Pop 'this' reference (if instance method call)
        popStack(1);

        // Pop arguments
        popStack(numArgs);

        // Push return value if any
        if (hasReturn) {
            pushStack(returnIsWide ? 2 : 1);
        }
    }

    // ========== DEBUGGING ==========

    /**
     * Get debug string representation.
     *
     * @return Debug info
     */
    @Override
    public String toString() {
        return String.format(
                "MethodContext{class=%s, method=%s, static=%b, " +
                        "currentStack=%d, maxStack=%d, maxLocals=%d}",
                classDecl.getName(), methodName, isStatic,
                currentStackDepth, maxStackDepth, getMaxLocals()
        );
    }

    /**
     * Get detailed state (for debugging).
     *
     * @return Detailed state string
     */
    public String getDetailedState() {
        StringBuilder sb = new StringBuilder();
        sb.append("MethodContext for ").append(classDecl.getName()).append(".")
                .append(methodName).append("\n");
        sb.append("  Static: ").append(isStatic).append("\n");
        sb.append("  Current stack depth: ").append(currentStackDepth).append("\n");
        sb.append("  Max stack depth: ").append(maxStackDepth).append("\n");
        sb.append("  Max locals: ").append(getMaxLocals()).append("\n");
        sb.append("  Variables:\n");

        locals.getAllMappings().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByValue())
                .forEach(entry ->
                        sb.append("    ").append(entry.getKey())
                                .append(" -> slot ").append(entry.getValue())
                                .append("\n")
                );

        return sb.toString();
    }
}
