package com.lowleveldesign.userfilemanagement.exception;

/**
 * Base type for all domain-specific exceptions thrown by the User and File
 * Management System. Being unchecked keeps the service API clean while still
 * allowing callers to catch {@code UserFileManagementException} to handle every
 * business-rule violation in one place.
 */
public class UserFileManagementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a detail message.
     *
     * @param message description of the violation
     */
    public UserFileManagementException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a detail message and underlying cause.
     *
     * @param message description of the violation
     * @param cause   the underlying cause
     */
    public UserFileManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
