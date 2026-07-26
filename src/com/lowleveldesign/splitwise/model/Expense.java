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

    public Expense(String description, double amount, User paidBy, SplitType splitType, List<Split> splits) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.splits = Collections.unmodifiableList(splits);
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("Expense[%s, amount=%.2f, paidBy=%s, splitType=%s]",
                description, amount, paidBy.getName(), splitType);
    }
}
