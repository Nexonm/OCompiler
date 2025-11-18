package codegen;

/**
 * Generates unique labels for control flow in Jasmin code.
 * Labels are descriptive and include context for easier debugging.
 *
 * Format: Label_N_ClassName_MethodName_Type
 * Example: Label_0_Counter_increment_IfThen
 */
public class LabelGenerator {

    private int counter;
    private final String className;
    private final String methodName;

    /**
     * Creates a label generator for a specific method context.
     *
     * @param className The name of the class being generated
     * @param methodName The name of the method being generated
     */
    public LabelGenerator(String className, String methodName) {
        this.counter = 0;
        this.className = className;
        this.methodName = methodName;
    }

    /**
     * Generates the next unique label with custom type.
     *
     * @param type Descriptive type (e.g., "IfThen", "WhileStart")
     * @return Unique label string
     */
    public String next(String type) {
        return String.format("Label_%d_%s_%s_%s",
                counter++, className, methodName, type);
    }

    // ===== Convenience Methods for Common Label Types =====

    /**
     * Generate label for if-then branch.
     */
    public String ifThen() {
        return next("IfThen");
    }

    /**
     * Generate label for if-else branch.
     */
    public String ifElse() {
        return next("IfElse");
    }

    /**
     * Generate label for end of if statement.
     */
    public String ifEnd() {
        return next("IfEnd");
    }

    /**
     * Generate label for while loop start.
     */
    public String whileStart() {
        return next("WhileStart");
    }

    /**
     * Generate label for while loop end.
     */
    public String whileEnd() {
        return next("WhileEnd");
    }

    /**
     * Generate label for boolean short-circuit evaluation.
     */
    public String boolShortCircuit() {
        return next("BoolShort");
    }

    /**
     * Generate generic label (for custom use).
     */
    public String generic() {
        return next("Generic");
    }

    /**
     * Get current counter value (for debugging).
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Reset counter (useful for testing).
     */
    public void reset() {
        this.counter = 0;
    }
}
