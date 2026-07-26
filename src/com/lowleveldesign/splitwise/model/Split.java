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

    /**
     * Private constructor; use the static factory methods instead.
     *
     * @param user    the participant this share belongs to
     * @param amount  the amount owed (final for EXACT, computed later otherwise)
     * @param percent the percentage of the total (only for PERCENT splits, else {@code null})
     */
    private Split(User user, double amount, Double percent) {
        this.user = user;
        this.amount = amount;
        this.percent = percent;
    }

    /**
     * Creates a share for an EQUAL split; the amount is filled in later by the strategy.
     *
     * @param user the participant
     * @return a new {@code Split} with no pre-set amount or percent
     */
    public static Split equalShare(User user) {
        return new Split(user, 0.0, null);
    }

    /**
     * Creates a share for an EXACT split with a caller-supplied amount.
     *
     * @param user   the participant
     * @param amount the exact amount this user owes
     * @return a new {@code Split} carrying the exact amount
     */
    public static Split exactShare(User user, double amount) {
        return new Split(user, amount, null);
    }

    /**
     * Creates a share for a PERCENT split with a caller-supplied percentage.
     *
     * @param user    the participant
     * @param percent this user's percentage of the total (0-100)
     * @return a new {@code Split} carrying the percent; amount is computed later
     */
    public static Split percentShare(User user, double percent) {
        return new Split(user, 0.0, percent);
    }

    /** @return the participant this share belongs to. */
    public User getUser() {
        return user;
    }

    /** @return the final amount this user owes for the expense. */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the final amount owed by this user. Called by the split strategy
     * once it has computed each participant's share.
     *
     * @param amount the amount owed
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /** @return the percentage of the total for PERCENT splits, or {@code null} otherwise. */
    public Double getPercent() {
        return percent;
    }

    /** @return a human-readable "{name} owes {amount}" representation. */
    @Override
    public String toString() {
        return user.getName() + " owes " + String.format("%.2f", amount);
    }
}
