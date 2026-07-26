package com.lowleveldesign.wallet.model;

import java.math.BigDecimal;

/**
 * An immutable record of a single movement of money in or out of a wallet, as
 * it appears on that wallet's statement.
 * <p>
 * The {@code source} is the counterparty of a transfer (the other account
 * holder) or the name of the offer that produced the reward, e.g.
 * {@code "Offer1"}, {@code "Offer2"} or {@code "FixedDeposit"}.
 */
public final class Transaction {

    private final String source;
    private final TransactionType type;
    private final BigDecimal amount;
    private final boolean transfer;

    /**
     * Creates a transaction.
     *
     * @param source   the counterparty or offer name shown on the statement
     * @param type     whether this credited or debited the wallet
     * @param amount   the amount moved (always positive)
     * @param transfer {@code true} if this arose from a {@code TransferMoney}
     *                 command; {@code false} for offer or interest rewards
     */
    public Transaction(String source, TransactionType type, BigDecimal amount, boolean transfer) {
        this.source = source;
        this.type = type;
        this.amount = amount;
        this.transfer = transfer;
    }

    /** @return the counterparty or offer name shown on the statement. */
    public String getSource() {
        return source;
    }

    /** @return whether this transaction credited or debited the wallet. */
    public TransactionType getType() {
        return type;
    }

    /** @return the amount moved (always positive). */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @return {@code true} if this transaction came from a money transfer, as
     *         opposed to an offer or fixed-deposit reward. Only transfers count
     *         towards a customer's transaction total for Offer2 and towards a
     *         fixed deposit's maturity.
     */
    public boolean isTransfer() {
        return transfer;
    }

    /** @return the statement line, e.g. {@code "Hermione credit 2"}. */
    @Override
    public String toString() {
        return source + " " + type.getLabel() + " " + Money.format(amount);
    }
}
