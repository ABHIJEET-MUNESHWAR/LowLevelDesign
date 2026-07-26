package com.lowleveldesign.wallet.exception;

/**
 * Thrown when a monetary amount is invalid: negative, below the smallest
 * transferable unit, or specified with more precision than the currency allows.
 */
public class InvalidAmountException extends WalletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of why the amount is invalid
     */
    public InvalidAmountException(String message) {
        super(message);
    }
}
