package lexer;

/**
 * Represents a span of text in the source code with precise positioning.
 * Provides line number and character range information for tokens.
 */
public record Span(int line, int start, int end) {

    /**
     * Creates a new span with validation.
     *
     * @param line  The line number (0-based)
     * @param start The starting column position (0-based)
     * @param end   The ending column position (0-based, exclusive)
     */
    public Span {
        if (line < 0) {
            throw new IllegalArgumentException("Error creating a new lexer.Span: line must be >= 0, got: " + line);
        }
        if (start < 0) {
            throw new IllegalArgumentException("Error creating a new lexer.Span: start must be >= 0, got: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("Error creating a new lexer.Span: end must be >= start, got start=" + start +
                    ", end=" + end);
        }
    }

    /**
     * Gets the length of this span in characters.
     *
     * @return The number of characters covered by this span
     */
    public int length() {
        return end - start;
    }

    /**
     * Creates a span for a single character.
     *
     * @param line     The line number
     * @param position The character position
     * @return A span covering one character
     */
    public static Span single(int line, int position) {
        return new Span(line, position, position + 1);
    }

    /**
     * Creates a zero-width span at a specific position.
     * Useful for synthetic/inserted tokens.
     *
     * @param line     The line number
     * @param position The position
     * @return A zero-width span
     */
    public static Span empty(int line, int position) {
        return new Span(line, position, position);
    }

    /**
     * Combines this span with another span to create a larger span. Works only with
     * spans that have the same line.
     *
     * @param other The other span to merge with
     * @return A new span covering both spans
     */
    public Span merge(Span other) {
        if (this.line != other.line) {
            throw new IllegalArgumentException("Error merging two spans: cannot merge spans from different lines");
        }
        return new Span(line, Math.min(start, other.start), Math.max(end, other.end));
    }

    /**
     * Checks if this span contains a specific position.
     *
     * @param line   The line to check
     * @param column The column to check
     * @return true if the position is within this span
     */
    public boolean contains(int line, int column) {
        return this.line == line && column >= start && column < end;
    }

    /**
     * Checks if this span overlaps with another span.
     *
     * @param other The other span
     * @return true if the spans overlap
     */
    public boolean overlaps(Span other) {
        if (this.line != other.line) {
            return false;
        }
        return start < other.end && end > other.start;
    }

    /**
     * Returns a formatted string representation suitable for error messages.
     * External output requires +1 addition.
     *
     * @return Formatted position string like "line 5, columns 10-15"
     */
    public String toErrorString() {
        if (length() <= 1) {
            return String.format("line %d, column %d", line + 1, start + 1);
        } else {
            return String.format("line %d, columns %d-%d", line + 1, start + 1, end);
        }
    }

    @Override
    public String toString() {
        return String.format("lexer.Span(%d:%d-%d)", line, start, end);
    }
}
