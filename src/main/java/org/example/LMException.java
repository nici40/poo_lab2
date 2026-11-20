package org.example;

/** The model server could not be reached or its answer could not be read. */
public class LMException extends Exception {
    private static final long serialVersionUID = 1L;

    public LMException(String message) {
        super(message);
    }

    public LMException(String message, Throwable cause) {
        super(message, cause);
    }
}
