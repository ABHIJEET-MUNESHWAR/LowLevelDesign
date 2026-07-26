package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when registering a user or creating an account whose username is
 * already taken by another user.
 */
public class UserAlreadyExistsException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param username the username that is already in use
     */
    public UserAlreadyExistsException(String username) {
        super("A user already exists with username: " + username);
    }
}
