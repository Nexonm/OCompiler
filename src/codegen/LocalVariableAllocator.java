package codegen;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages local variable slot allocation for JVM bytecode generation.
 *
 * Maps variable names to slot numbers according to JVM rules:
 * - Slot 0 = 'this' for instance methods
 * - Slots 1-N = method parameters
 * - Remaining slots = local variables
 *
 * Handles double-width types (double/Real takes 2 slots).
 */
public class LocalVariableAllocator {

    private final Map<String, Integer> variableToSlot;
    private int nextSlot;
    private final boolean isStatic;

    /**
     * Creates a local variable allocator.
     *
     * @param isStatic true if method is static, false if instance method
     */
    public LocalVariableAllocator(boolean isStatic) {
        this.variableToSlot = new HashMap<>();
        this.isStatic = isStatic;

        // Reserve slot 0 for 'this' if instance method
        this.nextSlot = isStatic ? 0 : 1; // But most tyme it is instance method (not static)

        // Add 'this' to map for instance methods
        if (!isStatic) {
            variableToSlot.put("this", 0);
        }
    }

    // ========== PARAMETER MANAGEMENT ==========

    /**
     * Add a parameter to the slot map.
     * Parameters are added in order and take consecutive slots.
     *
     * @param name Parameter name
     * @param isWide true if parameter is double-width (double/Real)
     * @return Allocated slot number
     */
    public int addParameter(String name, boolean isWide) {
        if (variableToSlot.containsKey(name)) {
            throw new IllegalArgumentException("Parameter already defined: " + name);
        }

        int slot = nextSlot;
        variableToSlot.put(name, slot);

        // Double-width types take 2 slots
        nextSlot += isWide ? 2 : 1;

        return slot;
    }

    /**
     * Add a parameter (convenience method, assumes single-width).
     *
     * @param name Parameter name
     * @return Allocated slot number
     */
    public int addParameter(String name) {
        return addParameter(name, false);
    }

    // ========== LOCAL VARIABLE MANAGEMENT ==========

    /**
     * Allocate a slot for a local variable.
     * If variable is already allocated, returns existing slot.
     *
     * @param name Variable name
     * @param isWide true if variable is double-width (double/Real)
     * @return Allocated slot number
     */
    public int allocate(String name, boolean isWide) {
        // If already allocated, return existing slot
        if (variableToSlot.containsKey(name)) {
            return variableToSlot.get(name);
        }

        int slot = nextSlot;
        variableToSlot.put(name, slot);

        // Double-width types take 2 slots
        nextSlot += isWide ? 2 : 1;

        return slot;
    }

    /**
     * Allocate a slot for a local variable (convenience, assumes single-width).
     *
     * @param name Variable name
     * @return Allocated slot number
     */
    public int allocate(String name) {
        return allocate(name, false);
    }

    // ========== SLOT LOOKUP ==========

    /**
     * Get the slot number for a variable.
     *
     * @param name Variable name
     * @return Slot number, or -1 if not found
     */
    public int getSlot(String name) {
        return variableToSlot.getOrDefault(name, -1);
    }

    /**
     * Check if a variable has been allocated.
     *
     * @param name Variable name
     * @return true if variable has a slot
     */
    public boolean hasVariable(String name) {
        return variableToSlot.containsKey(name);
    }

    /**
     * Get the slot for 'this' (only valid for instance methods).
     *
     * @return Slot 0, or -1 if static method
     */
    public int getThisSlot() {
        return isStatic ? -1 : 0;
    }

    // ========== METADATA ==========

    /**
     * Get total number of local variable slots used.
     * This is the value for .limit locals in Jasmin.
     *
     * @return Maximum locals count
     */
    public int getMaxLocals() {
        return nextSlot;
    }

    /**
     * Get number of variables allocated (including 'this').
     *
     * @return Count of variables
     */
    public int getVariableCount() {
        return variableToSlot.size();
    }

    /**
     * Check if this is a static method.
     *
     * @return true if static
     */
    public boolean isStatic() {
        return isStatic;
    }

    // ========== DEBUGGING ==========

    /**
     * Get all variable mappings (for debugging).
     *
     * @return Copy of variable-to-slot map
     */
    public Map<String, Integer> getAllMappings() {
        return new HashMap<>(variableToSlot);
    }

    /**
     * Clear all allocations (useful for testing).
     */
    public void clear() {
        variableToSlot.clear();
        nextSlot = isStatic ? 0 : 1;

        if (!isStatic) {
            variableToSlot.put("this", 0);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LocalVariableAllocator {\n");
        sb.append("  isStatic: ").append(isStatic).append("\n");
        sb.append("  maxLocals: ").append(nextSlot).append("\n");
        sb.append("  mappings:\n");

        variableToSlot.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry ->
                        sb.append("    ").append(entry.getKey())
                                .append(" -> slot ").append(entry.getValue())
                                .append("\n")
                );

        sb.append("}");
        return sb.toString();
    }
}
