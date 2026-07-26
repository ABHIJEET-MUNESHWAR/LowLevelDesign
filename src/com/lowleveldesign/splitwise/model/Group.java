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

    /**
     * Creates a group with a random {@code id} and the given name.
     *
     * @param name display name of the group
     */
    public Group(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /** @return the auto-generated unique identifier of this group. */
    public String getId() {
        return id;
    }

    /** @return the display name of this group. */
    public String getName() {
        return name;
    }

    /**
     * Adds a user to the group if not already a member (idempotent).
     *
     * @param user the user to add
     */
    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    /** @return an unmodifiable view of the group's members. */
    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * Appends an expense to this group's history.
     *
     * @param expense the expense to record
     */
    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    /** @return an unmodifiable view of the group's expense history. */
    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    /** @return a short {@code Group[name]} representation. */
    @Override
    public String toString() {
        return "Group[" + name + "]";
    }
}
