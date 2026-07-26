package com.lowleveldesign.wallet.model;

import com.lowleveldesign.wallet.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A digital wallet owned by a single account holder.
 * <p>
 * A wallet keeps its current balance (which may never go below zero), an
 * ordered history of every {@link Transaction} that touched it, a running count
 * of the transfer transactions it took part in (used for Offer2), the order in
 * which it was created (used as the final Offer2 tie-breaker) and an optional
 * {@link FixedDeposit}.
 */
public final class Wallet {

    private final String accountHolder;
    private final int creationOrder;
    private final List<Transaction> transactions = new ArrayList<>();
    private BigDecimal balance;
    private int transferCount;
    private FixedDeposit fixedDeposit;

    /**
     * Creates a wallet with an opening balance.
     *
     * @param accountHolder the name of the account holder
     * @param openingBalance the opening balance (already validated as &gt;= 0)
     * @param creationOrder  a monotonically increasing sequence number capturing
     *                       the order in which wallets were created
     */
    public Wallet(String accountHolder, BigDecimal openingBalance, int creationOrder) {
        this.accountHolder = accountHolder;
        this.balance = Money.normalize(openingBalance);
        this.creationOrder = creationOrder;
    }

    /** @return the account holder's name. */
    public String getAccountHolder() {
        return accountHolder;
    }

    /** @return the current balance. */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @return the creation sequence number (lower means created earlier). */
    public int getCreationOrder() {
        return creationOrder;
    }

    /** @return the number of transfer transactions this wallet took part in. */
    public int getTransferCount() {
        return transferCount;
    }

    /** @return the active fixed deposit, or {@code null} if none. */
    public FixedDeposit getFixedDeposit() {
        return fixedDeposit;
    }

    /**
     * Sets (or clears) the active fixed deposit.
     *
     * @param fixedDeposit the new deposit, or {@code null} to clear it
     */
    public void setFixedDeposit(FixedDeposit fixedDeposit) {
        this.fixedDeposit = fixedDeposit;
    }

    /** @return an unmodifiable, chronologically ordered view of the statement. */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Adds money to the wallet and records the movement on its statement.
     *
     * @param source   the counterparty or offer name
     * @param amount   the positive amount to add
     * @param transfer whether this arose from a money transfer
     */
    public void credit(String source, BigDecimal amount, boolean transfer) {
        balance = Money.normalize(balance.add(amount));
        record(source, TransactionType.CREDIT, amount, transfer);
    }

    /**
     * Removes money from the wallet and records the movement on its statement.
     *
     * @param source   the counterparty or offer name
     * @param amount   the positive amount to remove
     * @param transfer whether this arose from a money transfer
     * @throws InsufficientBalanceException if the wallet does not hold enough
     */
    public void debit(String source, BigDecimal amount, boolean transfer) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountHolder);
        }
        balance = Money.normalize(balance.subtract(amount));
        record(source, TransactionType.DEBIT, amount, transfer);
    }

    private void record(String source, TransactionType type, BigDecimal amount, boolean transfer) {
        transactions.add(new Transaction(source, type, amount, transfer));
        if (transfer) {
            transferCount++;
        }
    }
}
