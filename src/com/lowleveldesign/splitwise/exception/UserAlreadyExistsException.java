package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when trying to register a user whose id is already in use.
 */
public class UserAlreadyExistsException extends SplitwiseException {
    public UserAlreadyExistsException(String userId) {
        super("A user is already registered with id: " + userId);
    }
}
