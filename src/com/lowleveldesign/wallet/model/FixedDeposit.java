package com.lowleveldesign.wallet.model;

import java.math.BigDecimal;

/**
 * A fixed deposit parked against a wallet.
 * <p>
 * When created, an amount is parked. Over the next
 * {@link #REQUIRED_TRANSACTIONS} transfer transactions the wallet's balance must
 * remain at or above the parked amount. If it does, the holder earns
 * {@link #INTEREST} as interest and the deposit matures. If the balance ever
 * drops below the parked amount the deposit is dissolved immediately and a new
 * one must be opened.
 */
public final class FixedDeposit {

    /** Number of qualifying transactions the balance must survive. */
    public static final int REQUIRED_TRANSACTIONS = 5;

    /** Interest paid on maturity: F&#8377; 10. */
    public static final BigDecimal INTEREST = new BigDecimal("10");

    private final BigDecimal amount;
    private int remainingTransactions;

    /**
     * Opens a new fixed deposit.
     *
     * @param amount the amount to park
     */
    public FixedDeposit(BigDecimal amount) {
        this.amount = amount;
        this.remainingTransactions = REQUIRED_TRANSACTIONS;
    }

    /** @return the parked amount that the balance must stay at or above. */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return how many qualifying transactions remain before maturity. */
    public int getRemainingTransactions() {
        return remainingTransactions;
    }

    /**
     * Records that one qualifying transaction has elapsed.
     *
     * @return {@code true} if the deposit has now matured (no transactions left)
     */
    public boolean countTransaction() {
        if (remainingTransactions > 0) {
            remainingTransactions--;
        }
        return remainingTransactions == 0;
    }
}
