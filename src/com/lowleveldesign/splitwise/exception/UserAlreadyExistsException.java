package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when trying to register a user whose id is already in use.
 */
public class UserAlreadyExistsException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param userId the id that is already registered
     */
    public UserAlreadyExistsException(String userId) {
        super("A user is already registered with id: " + userId);
    }
}
