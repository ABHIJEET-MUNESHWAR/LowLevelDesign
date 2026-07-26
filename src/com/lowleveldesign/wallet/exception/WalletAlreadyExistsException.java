package com.lowleveldesign.wallet.exception;

/**
 * Thrown when attempting to create a wallet whose account holder name is
 * already registered.
 */
public class WalletAlreadyExistsException extends WalletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param accountHolder the name that already owns a wallet
     */
    public WalletAlreadyExistsException(String accountHolder) {
        super("A wallet already exists for account holder: " + accountHolder);
    }
}
