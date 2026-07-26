package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an expense's splits are invalid, e.g. EXACT split amounts
 * don't add up to the total expense amount, or PERCENT splits don't add
 * up to 100%.
 */
public class InvalidSplitException extends RuntimeException {
    public InvalidSplitException(String message) {
        super(message);
    }
}
