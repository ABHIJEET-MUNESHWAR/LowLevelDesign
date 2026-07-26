package com.lowleveldesign.splitwise.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single expense paid by one user and shared among a list of
 * {@link Split}s (participants and the amount each of them owes).
 */
public class Expense {

    private final String id;
    private final String description;
    private final double amount;
    private final User paidBy;
    private final SplitType splitType;
    private final List<Split> splits;
    private final LocalDateTime createdAt;

    /**
     * Creates an immutable expense. A random {@code id} and the current
     * timestamp are assigned automatically, and the splits list is wrapped
     * as unmodifiable.
     *
     * @param description short description of the expense
     * @param amount      total amount of the expense
     * @param paidBy      the user who paid the bill
     * @param splitType   how the amount is split among participants
     * @param splits      each participant's share (already computed/validated)
     */
    public Expense(String description, double amount, User paidBy, SplitType splitType, List<Split> splits) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.splits = Collections.unmodifiableList(splits);
        this.createdAt = LocalDateTime.now();
    }

    /** @return the auto-generated unique identifier of this expense. */
    public String getId() {
        return id;
    }

    /** @return the description of this expense. */
    public String getDescription() {
        return description;
    }

    /** @return the total amount of this expense. */
    public double getAmount() {
        return amount;
    }

    /** @return the user who paid for this expense. */
    public User getPaidBy() {
        return paidBy;
    }

    /** @return the split strategy type used for this expense. */
    public SplitType getSplitType() {
        return splitType;
    }

    /** @return an unmodifiable list of each participant's share. */
    public List<Split> getSplits() {
        return splits;
    }

    /** @return the timestamp at which this expense was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return a concise summary of the expense for logging/debugging. */
    @Override
    public String toString() {
        return String.format("Expense[%s, amount=%.2f, paidBy=%s, splitType=%s]",
                description, amount, paidBy.getName(), splitType);
    }
}
