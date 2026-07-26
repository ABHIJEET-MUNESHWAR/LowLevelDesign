package com.lowleveldesign.splitwise;

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
        Arrays.asList(alice, bob, charlie, dave).forEach(splitwise::registerUser);

        Group trip = splitwise.createGroup("Goa Trip", Arrays.asList(alice, bob, charlie, dave));

        // 1) EQUAL split: Alice pays 4000 for dinner, split equally among all 4.
        List<Split> equalSplits = Arrays.asList(
                Split.equalShare(alice), Split.equalShare(bob), Split.equalShare(charlie), Split.equalShare(dave));
        splitwise.addExpenseToGroup(trip, "Dinner", 4000, alice, equalSplits, SplitType.EQUAL);

        // 2) EXACT split: Bob pays 1500 for cabs; exact shares specified.
        List<Split> exactSplits = Arrays.asList(
                Split.exactShare(alice, 300), Split.exactShare(bob, 500),
                Split.exactShare(charlie, 400), Split.exactShare(dave, 300));
        splitwise.addExpenseToGroup(trip, "Cabs", 1500, bob, exactSplits, SplitType.EXACT);

        // 3) PERCENT split: Charlie pays 2000 for the hotel room.
        List<Split> percentSplits = Arrays.asList(
                Split.percentShare(alice, 25.0), Split.percentShare(bob, 25.0),
                Split.percentShare(charlie, 25.0), Split.percentShare(dave, 25.0));
        splitwise.addExpenseToGroup(trip, "Hotel", 2000, charlie, percentSplits, SplitType.PERCENT);

        System.out.println("---- Balances after all expenses ----");
        splitwise.showAllBalances();

        System.out.println("\n---- Simplified group settlement ----");
        splitwise.simplifyGroupDebts(trip);

        System.out.println("\n---- Dave settles up 100 with Alice ----");
        splitwise.settleUp(dave, alice, 100);
        splitwise.showBalances(alice);
    }
}
