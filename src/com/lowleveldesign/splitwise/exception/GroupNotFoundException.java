package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an operation references a group that does not exist in the
 * {@code SplitwiseService}.
 */
public class GroupNotFoundException extends SplitwiseException {
    public GroupNotFoundException(String groupId) {
        super("No group found with id: " + groupId);
    }
}
