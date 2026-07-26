package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when an expense's splits are invalid, e.g. EXACT split amounts
 * don't add up to the total expense amount, or PERCENT splits don't add
 * up to 100%.
 */
public class InvalidSplitException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param message description of why the split input is invalid
     */
    public InvalidSplitException(String message) {
        super(message);
    }
}
