package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an expense itself is invalid regardless of how it is split,
 * e.g. a non-positive amount, a missing payer, or no participants.
 */
public class InvalidExpenseException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param message description of why the expense is invalid
     */
    public InvalidExpenseException(String message) {
        super(message);
    }
}
