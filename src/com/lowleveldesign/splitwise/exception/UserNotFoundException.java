package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an operation references a user who has not been registered
 * with the {@code SplitwiseService}.
 */
public class UserNotFoundException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param userId the id of the user that could not be found
     */
    public UserNotFoundException(String userId) {
        super("No registered user found with id: " + userId);
    }
}
