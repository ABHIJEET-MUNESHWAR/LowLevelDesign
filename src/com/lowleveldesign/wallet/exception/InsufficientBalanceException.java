package com.lowleveldesign.wallet.exception;

/**
 * Thrown when a transfer would drive an account's balance below zero, which is
 * never permitted.
 */
public class InsufficientBalanceException extends WalletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param accountHolder the account holder lacking sufficient funds
     */
    public InsufficientBalanceException(String accountHolder) {
        super("Insufficient balance in the wallet of: " + accountHolder);
    }
}
