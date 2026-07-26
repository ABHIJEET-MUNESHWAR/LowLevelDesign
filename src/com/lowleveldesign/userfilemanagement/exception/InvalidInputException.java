package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when a supplied argument is missing, blank or otherwise invalid, such
 * as a blank username, an empty password or a null file name.
 */
public class InvalidInputException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of what was invalid about the input
     */
    public InvalidInputException(String message) {
        super(message);
    }
}
