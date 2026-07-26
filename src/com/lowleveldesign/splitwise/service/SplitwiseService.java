package com.lowleveldesign.splitwise.service;

import com.lowleveldesign.splitwise.exception.GroupNotFoundException;
import com.lowleveldesign.splitwise.exception.InvalidExpenseException;
import com.lowleveldesign.splitwise.exception.InvalidSettlementException;
import com.lowleveldesign.splitwise.exception.UserAlreadyExistsException;
import com.lowleveldesign.splitwise.exception.UserNotFoundException;
import com.lowleveldesign.splitwise.exception.UserNotInGroupException;
import com.lowleveldesign.splitwise.model.Expense;
import com.lowleveldesign.splitwise.model.Group;
import com.lowleveldesign.splitwise.model.Split;
import com.lowleveldesign.splitwise.model.SplitType;
import com.lowleveldesign.splitwise.model.User;
import com.lowleveldesign.splitwise.split.SplitStrategy;
import com.lowleveldesign.splitwise.split.SplitStrategyFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Facade that ties together users, groups, expenses and the balance sheet.
 * This is the main entry point client code should use.
 */
public class SplitwiseService {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    private final BalanceSheet balanceSheet = new BalanceSheet();

    public void registerUser(User user) {
        if (user == null) {
            throw new InvalidExpenseException("User must not be null");
        }
        if (users.containsKey(user.getId())) {
            throw new UserAlreadyExistsException(user.getId());
        }
        users.put(user.getId(), user);
    }

    public Group createGroup(String name, List<User> members) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidExpenseException("Group name must not be empty");
        }
        if (members == null || members.isEmpty()) {
            throw new InvalidExpenseException("A group must have at least one member");
        }
        Group group = new Group(name);
        for (User member : members) {
            requireRegistered(member);
            group.addMember(member);
        }
        groups.put(group.getId(), group);
        return group;
    }

    private void requireRegistered(User user) {
        if (user == null) {
            throw new UserNotFoundException("null");
        }
        if (!users.containsKey(user.getId())) {
            throw new UserNotFoundException(user.getId());
        }
    }

    /**
     * Adds a standalone expense (not tied to a group) and updates the
     * balance sheet accordingly.
     *
     * @param description description of the expense
     * @param amount      total amount of the expense
     * @param paidBy      user who paid the bill
     * @param splits      participants' shares (see {@link Split} factory methods)
     * @param splitType   how the amount should be split
     */
    public Expense addExpense(String description, double amount, User paidBy, List<Split> splits, SplitType splitType) {
        if (amount <= 0) {
            throw new InvalidExpenseException("Expense amount must be positive");
        }
        if (splits == null || splits.isEmpty()) {
            throw new InvalidExpenseException("An expense must have at least one participant");
        }
        requireRegistered(paidBy);
        for (Split split : splits) {
            requireRegistered(split.getUser());
        }

        SplitStrategy strategy = SplitStrategyFactory.getStrategy(splitType);
        strategy.validateAndCompute(amount, splits);

        Expense expense = new Expense(description, amount, paidBy, splitType, splits);
        for (Split split : splits) {
            if (!split.getUser().equals(paidBy)) {
                balanceSheet.recordDebt(paidBy, split.getUser(), split.getAmount());
            }
        }
        return expense;
    }

    /** Adds an expense and also records it against the given group's history. */
    public Expense addExpenseToGroup(Group group, String description, double amount, User paidBy,
                                      List<Split> splits, SplitType splitType) {
        if (group == null || !groups.containsKey(group.getId())) {
            throw new GroupNotFoundException(group == null ? "null" : group.getId());
        }
        requireGroupMember(group, paidBy);
        if (splits != null) {
            for (Split split : splits) {
                requireGroupMember(group, split.getUser());
            }
        }
        Expense expense = addExpense(description, amount, paidBy, splits, splitType);
        group.addExpense(expense);
        return expense;
    }

    private void requireGroupMember(Group group, User user) {
        if (user == null || !group.getMembers().contains(user)) {
            throw new UserNotInGroupException(user == null ? "null" : user.getId(), group.getName());
        }
    }

    /** Records a cash settlement: {@code payer} pays {@code payee} the given amount. */
    public void settleUp(User payer, User payee, double amount) {
        requireRegistered(payer);
        requireRegistered(payee);
        if (payer.equals(payee)) {
            throw new InvalidSettlementException("A user cannot settle up with themselves");
        }
        if (amount <= 0) {
            throw new InvalidSettlementException("Settlement amount must be positive");
        }
        balanceSheet.settle(payer, payee, amount);
    }

    public void showBalances(User user) {
        Map<String, Double> nonZero = balanceSheet.getNonZeroBalances(user);
        if (nonZero.isEmpty()) {
            System.out.println(user.getName() + " has no outstanding balances.");
            return;
        }
        nonZero.forEach((otherId, amount) -> {
            User other = users.get(otherId);
            String otherName = other != null ? other.getName() : otherId;
            if (amount > 0) {
                System.out.printf("%s owes %s: %.2f%n", otherName, user.getName(), amount);
            } else {
                System.out.printf("%s owes %s: %.2f%n", user.getName(), otherName, -amount);
            }
        });
    }

    public void showAllBalances() {
        users.values().forEach(this::showBalances);
    }

    /**
     * Simplifies debts among a group's members so that the number of
     * transactions required to settle everyone up is minimized. Uses a
     * greedy approach: repeatedly match the biggest creditor with the
     * biggest debtor.
     */
    public void simplifyGroupDebts(Group group) {
        if (group == null || !groups.containsKey(group.getId())) {
            throw new GroupNotFoundException(group == null ? "null" : group.getId());
        }
        Map<User, Double> net = new HashMap<>();
        List<User> members = group.getMembers();
        for (User u : members) {
            net.put(u, balanceSheet.getNetBalance(u));
        }

        PriorityQueue<Map.Entry<User, Double>> creditors =
                new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<User, Double>> debtors =
                new PriorityQueue<>((a, b) -> Double.compare(a.getValue(), b.getValue()));

        for (Map.Entry<User, Double> e : net.entrySet()) {
            if (e.getValue() > 0.01) {
                creditors.add(e);
            } else if (e.getValue() < -0.01) {
                debtors.add(e);
            }
        }

        Deque<Map.Entry<User, Double>> creditorStack = new ArrayDeque<>();
        while (!creditors.isEmpty()) {
            creditorStack.addLast(creditors.poll());
        }
        Deque<Map.Entry<User, Double>> debtorStack = new ArrayDeque<>();
        while (!debtors.isEmpty()) {
            debtorStack.addLast(debtors.poll());
        }

        System.out.println("Simplified settlement plan for group " + group.getName() + ":");
        while (!creditorStack.isEmpty() && !debtorStack.isEmpty()) {
            Map.Entry<User, Double> creditor = creditorStack.peekFirst();
            Map.Entry<User, Double> debtor = debtorStack.peekFirst();
            double settledAmount = Math.min(creditor.getValue(), -debtor.getValue());
            settledAmount = Math.round(settledAmount * 100) / 100.0;

            System.out.printf("%s pays %s: %.2f%n", debtor.getKey().getName(), creditor.getKey().getName(), settledAmount);

            creditor.setValue(Math.round((creditor.getValue() - settledAmount) * 100) / 100.0);
            debtor.setValue(Math.round((debtor.getValue() + settledAmount) * 100) / 100.0);

            if (Math.abs(creditor.getValue()) < 0.01) {
                creditorStack.pollFirst();
            }
            if (Math.abs(debtor.getValue()) < 0.01) {
                debtorStack.pollFirst();
            }
        }
    }
}
