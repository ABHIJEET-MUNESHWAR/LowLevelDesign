package com.lowleveldesign.wallet.exception;

/**
 * Thrown when a command string cannot be parsed or has the wrong number or
 * type of arguments.
 */
public class InvalidCommandException extends WalletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of why the command is invalid
     */
    public InvalidCommandException(String message) {
        super(message);
    }
}
