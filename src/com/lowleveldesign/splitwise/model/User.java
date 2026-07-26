package com.lowleveldesign.splitwise.model;

import java.util.Objects;

/**
 * Represents a user of the Splitwise application.
 */
public class User {

    private final String id;
    private final String name;
    private final String email;

    /**
     * Creates a user.
     *
     * @param id    unique identifier of the user
     * @param name  display name of the user
     * @param email contact email of the user
     */
    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** @return the user's unique identifier. */
    public String getId() {
        return id;
    }

    /** @return the user's display name. */
    public String getName() {
        return name;
    }

    /** @return the user's email address. */
    public String getEmail() {
        return email;
    }

    /**
     * Two users are equal if and only if they share the same {@code id}.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code User} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    /** @return a hash code derived solely from the user's {@code id}. */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** @return a short {@code name(id)} representation of the user. */
    @Override
    public String toString() {
        return name + "(" + id + ")";
    }
}
