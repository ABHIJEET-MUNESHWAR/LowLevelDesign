package com.lowleveldesign.splitwise;

import com.lowleveldesign.splitwise.model.Expense;
import com.lowleveldesign.splitwise.model.Group;
import com.lowleveldesign.splitwise.model.Split;
import com.lowleveldesign.splitwise.model.SplitType;
import com.lowleveldesign.splitwise.model.User;
import com.lowleveldesign.splitwise.service.SplitwiseService;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates the Splitwise LLD: EQUAL, EXACT and PERCENT splits, checking
 * balances, settling up and simplifying group debts.
 */
public class Main {

    /**
     * Runs the Splitwise demonstration scenario.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SplitwiseService splitwise = new SplitwiseService();

        User alice = new User("u1", "Alice", "alice@mail.com");
        User bob = new User("u2", "Bob", "bob@mail.com");
        User charlie = new User("u3", "Charlie", "charlie@mail.com");
        User dave = new User("u4", "Dave", "dave@mail.com");
        List<User> everyone = Arrays.asList(alice, bob, charlie, dave);
        everyone.forEach(splitwise::registerUser);

        System.out.println("---- Registered users ----");
        everyone.forEach(u -> System.out.printf("%s <%s>%n", u, u.getEmail()));

        Group trip = splitwise.createGroup("Goa Trip", everyone);

        // 1) EQUAL split: Alice pays 4000 for dinner, split equally among all 4.
        List<Split> equalSplits = Arrays.asList(
                Split.equalShare(alice), Split.equalShare(bob), Split.equalShare(charlie), Split.equalShare(dave));
        Expense dinner = splitwise.addExpenseToGroup(trip, "Dinner", 4000, alice, equalSplits, SplitType.EQUAL);

        // 2) EXACT split: Bob pays 1500 for cabs; exact shares specified.
        List<Split> exactSplits = Arrays.asList(
                Split.exactShare(alice, 300), Split.exactShare(bob, 500),
                Split.exactShare(charlie, 400), Split.exactShare(dave, 300));
        Expense cabs = splitwise.addExpenseToGroup(trip, "Cabs", 1500, bob, exactSplits, SplitType.EXACT);

        // 3) PERCENT split: Charlie pays 2000 for the hotel room.
        List<Split> percentSplits = Arrays.asList(
                Split.percentShare(alice, 25.0), Split.percentShare(bob, 25.0),
                Split.percentShare(charlie, 25.0), Split.percentShare(dave, 25.0));
        Expense hotel = splitwise.addExpenseToGroup(trip, "Hotel", 2000, charlie, percentSplits, SplitType.PERCENT);

        System.out.println("\n---- " + trip + " expense history ----");
        for (Expense e : trip.getExpenses()) {
            System.out.println(describe(e));
        }
        System.out.println("(Latest expense id: " + hotel.getId() + ", added at " + hotel.getCreatedAt() + ")");
        System.out.println("First two expenses: " + dinner.getDescription() + ", " + cabs.getDescription());

        System.out.println("\n---- Balances after all expenses ----");
        splitwise.showAllBalances();

        System.out.printf("%nDirect query - Dave owes Alice: %.2f%n", -splitwise.getBalance(dave, alice));

        System.out.println("\n---- Simplified group settlement ----");
        splitwise.simplifyGroupDebts(trip);

        System.out.println("\n---- Dave settles up 100 with Alice ----");
        splitwise.settleUp(dave, alice, 100);
        splitwise.showBalances(alice);
        System.out.printf("Dave now owes Alice: %.2f%n", -splitwise.getBalance(dave, alice));
    }

    /**
     * Builds a detailed, human-readable description of an expense using its
     * accessors, including the per-participant breakdown.
     *
     * @param e the expense to describe
     * @return a multi-field summary string
     */
    private static String describe(Expense e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e).append(" [id=").append(e.getId()).append("]");
        sb.append(" paidBy=").append(e.getPaidBy().getName());
        sb.append(" total=").append(String.format("%.2f", e.getAmount()));
        sb.append(" type=").append(e.getSplitType());
        sb.append(" -> ");
        List<Split> splits = e.getSplits();
        for (int i = 0; i < splits.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(splits.get(i));
        }
        return sb.toString();
    }
}
