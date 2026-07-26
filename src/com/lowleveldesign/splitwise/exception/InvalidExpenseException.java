package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an expense itself is invalid regardless of how it is split,
 * e.g. a non-positive amount, a missing payer, or no participants.
 */
public class InvalidExpenseException extends SplitwiseException {
    public InvalidExpenseException(String message) {
        super(message);
    }
}
