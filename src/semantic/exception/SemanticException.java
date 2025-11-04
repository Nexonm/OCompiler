package semantic.exception;

/**
 * Exception thrown when semantic errors are detected.
 */
public class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }

    public SemanticException(String message, Throwable cause) {
        super(message, cause);
    }
}

