package com.lowleveldesign.wallet.exception;

/**
 * Thrown when an operation references an account holder that has no wallet.
 */
public class WalletNotFoundException extends WalletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param accountHolder the name for which no wallet was found
     */
    public WalletNotFoundException(String accountHolder) {
        super("No wallet found for account holder: " + accountHolder);
    }
}
