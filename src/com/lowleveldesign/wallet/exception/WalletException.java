package com.lowleveldesign.wallet.exception;

/**
 * Base type for all domain-specific exceptions thrown by the digital wallet
 * application. Being unchecked keeps the API clean while still allowing callers
 * to catch {@code WalletException} to handle every business-rule violation in
 * one place.
 */
public class WalletException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a detail message.
     *
     * @param message description of the violation
     */
    public WalletException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a detail message and underlying cause.
     *
     * @param message description of the violation
     * @param cause   the underlying cause
     */
    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
