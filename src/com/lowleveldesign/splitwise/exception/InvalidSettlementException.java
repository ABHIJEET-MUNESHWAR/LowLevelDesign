package com.lowleveldesign.splitwise.exception;

/**
 * Thrown when a cash settlement is invalid, e.g. a non-positive amount or
 * a user trying to settle up with themselves.
 */
public class InvalidSettlementException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param message description of why the settlement is invalid
     */
    public InvalidSettlementException(String message) {
        super(message);
    }
}
