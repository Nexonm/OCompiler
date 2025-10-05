package lexer;

/**
 * Represents a span of text in the source code with precise positioning.
 * Provides line number and character range information for tokens.
 */
public record Span(int line, int start, int endLine, int end) {

    /**
     * Creates a new span with validation.
     *
     * @param line  The line number (0-based)
     * @param start The starting column position (0-based)
     * @param end   The ending column position (0-based, exclusive)
     */
    public Span {
        if (line < 0) {
            throw new IllegalArgumentException("Error creating a new Span: line must be >= 0, got: " + line);
        }
        if (start < 0) {
            throw new IllegalArgumentException("Error creating a new Span: start must be >= 0, got: " + start);
        }
        if (endLine < 0) {
            throw new IllegalArgumentException(
                    "Error creating Span: endLine must be >= 0, got: " + endLine);
        }
        if (endLine < line) {
            throw new IllegalArgumentException(
                    "Error creating Span: endLine cannot be before startLine");
        }
        if (end < start) {
            throw new IllegalArgumentException("Error creating a new Span: end must be >= start, got start=" + start +
                    ", end=" + end);
        }
        if (endLine == line && end < start) {
            throw new IllegalArgumentException(
                    "Error creating Span: on same line, endColumn must be >= startColumn");
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
     * Creates a single-line span (old constructor for backward compatibility).
     *
     * @param line The line number (0-based)
     * @param start The starting column
     * @param end The ending column
     * @return Single-line span
     */
    public static Span singleLine(int line, int start, int end) {
        return new Span(line, start, line, end);
    }

    /**
     * Creates a span for a single character.
     *
     * @param line     The line number
     * @param position The character position
     * @return A span covering one character
     */
    public static Span single(int line, int position) {
        return new Span(line, position, line, position + 1);
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
        return new Span(line, position, line, position);
    }

    /**
     * Combines this span with another span to create a larger span.
     *
     * @param other The other span to merge with
     * @return A new span covering both spans
     */
    public Span merge(Span other) {
        // Find earliest start
        int newStartLine = Math.min(this.line, other.line);
        int newStartColumn;
        if (this.line < other.line) {
            newStartColumn = this.start;
        } else if (this.line > other.line) {
            newStartColumn = other.start;
        } else {
            // Same start line, take minimum column
            newStartColumn = Math.min(this.start, other.start);
        }

        // Find latest end
        int newEndLine = Math.max(this.endLine, other.endLine);
        int newEndColumn;
        if (this.endLine > other.endLine) {
            newEndColumn = this.end;
        } else if (this.endLine < other.endLine) {
            newEndColumn = other.end;
        } else {
            // Same end line, take maximum column
            newEndColumn = Math.max(this.end, other.end);
        }

        return new Span(newStartLine, newStartColumn, newEndLine, newEndColumn);
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
        return String.format("%d:%d-%d", line+1, start+1, end);
    }
}
