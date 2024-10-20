package com.github.phoswald.rstm.databind;

public class DatabinderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DatabinderException(String message) {
        super(message);
    }

    DatabinderException(Throwable cause) {
        super(cause);
    }
}
