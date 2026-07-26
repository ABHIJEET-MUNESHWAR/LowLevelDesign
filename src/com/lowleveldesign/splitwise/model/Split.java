package com.lowleveldesign.splitwise.model;

/**
 * Represents one user's share of an {@link Expense}.
 * <p>
 * Depending on the {@link SplitType} used to create the expense, either
 * {@code percent} (PERCENT split) is supplied up front, or {@code amount}
 * is supplied up front (EXACT split), or neither is supplied (EQUAL split).
 * The {@link com.lowleveldesign.splitwise.split.SplitStrategy} is responsible
 * for validating the input and filling in {@code amount} (the final amount
 * owed by {@code user} for this expense).
 */
public class Split {

    private final User user;
    private double amount;
    private final Double percent;

    private Split(User user, double amount, Double percent) {
        this.user = user;
        this.amount = amount;
        this.percent = percent;
    }

    public static Split equalShare(User user) {
        return new Split(user, 0.0, null);
    }

    public static Split exactShare(User user, double amount) {
        return new Split(user, amount, null);
    }

    public static Split percentShare(User user, double percent) {
        return new Split(user, 0.0, percent);
    }

    public User getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Double getPercent() {
        return percent;
    }

    @Override
    public String toString() {
        return user.getName() + " owes " + String.format("%.2f", amount);
    }
}
