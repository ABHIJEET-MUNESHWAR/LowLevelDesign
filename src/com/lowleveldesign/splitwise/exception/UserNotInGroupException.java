package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when a group expense references a user who is not a member of that
 * group (either as the payer or as a participant in the split).
 */
public class UserNotInGroupException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param userId    the id of the user that is not a member
     * @param groupName the name of the group in question
     */
    public UserNotInGroupException(String userId, String groupName) {
        super("User " + userId + " is not a member of group: " + groupName);
    }
}
