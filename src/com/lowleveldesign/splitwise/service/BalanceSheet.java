package com.lowleveldesign.splitwise.service;

import com.lowleveldesign.splitwise.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a pairwise ledger of who-owes-whom.
 * <p>
 * {@code balances.get(a).get(b) > 0} means {@code b} owes {@code a} that amount.
 * The relationship is kept symmetric: {@code balances[a][b] == -balances[b][a]}.
 */
public class BalanceSheet {

    private final Map<String, Map<String, Double>> balances = new HashMap<>();
    private static final double EPSILON = 0.001;

    /**
     * Records that {@code debtor} owes {@code creditor} the given amount
     * (e.g. because creditor paid for an expense on debtor's behalf). The
     * ledger is kept symmetric; a self-debt or zero amount is ignored.
     *
     * @param creditor the user who is owed money
     * @param debtor   the user who owes money
     * @param amount   the amount the debtor owes the creditor
     */
    public void recordDebt(User creditor, User debtor, double amount) {
        if (creditor.equals(debtor) || amount == 0.0) {
            return;
        }
        adjust(creditor.getId(), debtor.getId(), amount);
        adjust(debtor.getId(), creditor.getId(), -amount);
    }

    /**
     * Adds {@code delta} to the balance {@code a} holds against {@code b},
     * creating the inner map on first use.
     *
     * @param a     the id of the user whose row is updated
     * @param b     the id of the counterparty
     * @param delta the signed amount to add to the current balance
     */
    private void adjust(String a, String b, double delta) {
        balances.computeIfAbsent(a, k -> new HashMap<>())
                .merge(b, delta, Double::sum);
    }

    /**
     * Returns the current balance between two users.
     *
     * @param a the reference user
     * @param b the counterparty
     * @return the amount {@code b} owes {@code a}; a negative value means
     * {@code a} owes {@code b} instead
     */
    public double getBalance(User a, User b) {
        return balances.getOrDefault(a.getId(), new HashMap<>()).getOrDefault(b.getId(), 0.0);
    }

    /**
     * Settles up (partially or fully) the debt between two users, e.g. when
     * one user pays the other back in cash.
     *
     * @param payer  the user making the payment
     * @param payee  the user receiving the payment
     * @param amount amount being settled
     */
    public void settle(User payer, User payee, double amount) {
        // payer is paying off what they owe to payee, so payee's claim on payer decreases.
        recordDebt(payer, payee, amount);
    }

    /**
     * Returns the user's balances against every counterparty, excluding any
     * that have effectively settled to zero.
     *
     * @param user the user whose balances to fetch
     * @return a map of counterparty user-id to balance (positive means the
     * counterparty owes {@code user}, negative means {@code user} owes them)
     */
    public Map<String, Double> getNonZeroBalances(User user) {
        Map<String, Double> row = balances.getOrDefault(user.getId(), new HashMap<>());
        Map<String, Double> result = new HashMap<>();
        row.forEach((otherId, amount) -> {
            if (Math.abs(amount) >= EPSILON) {
                result.put(otherId, amount);
            }
        });
        return result;
    }

    /**
     * Returns the net position of a user across all counterparties.
     *
     * @param user the user whose net balance to compute
     * @return the net balance (positive means the user is owed money overall,
     * negative means they owe money overall)
     */
    public double getNetBalance(User user) {
        return balances.getOrDefault(user.getId(), new HashMap<>()).values()
                .stream().mapToDouble(Double::doubleValue).sum();
    }
}
