package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when an operation references a user who is not registered with the
 * system, identified either by id or by username.
 */
public class UserNotFoundException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param identifier the id or username that could not be resolved
     */
    public UserNotFoundException(String identifier) {
        super("No user found for: " + identifier);
    }
}
