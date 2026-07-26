package com.lowleveldesign.splitwise.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A group of users that share expenses together (e.g. "Goa Trip", "Flatmates").
 */
public class Group {

    private final String id;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();

    public Group(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    @Override
    public String toString() {
        return "Group[" + name + "]";
    }
}
