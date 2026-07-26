package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when an authenticated user attempts an operation they are not
 * authorized to perform, such as reading a file they have not been granted
 * access to, or a non-administrator invoking an administrative action.
 */
public class AccessDeniedException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the denied operation
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
