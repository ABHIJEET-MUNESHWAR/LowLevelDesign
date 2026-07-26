package com.lowleveldesign.userfilemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an account in the User and File Management System.
 *
 * <p>A user carries authentication data (a salted password hash, never the raw
 * password), profile data (email and display name), an authorization
 * {@link Role}, and an {@code active} flag that administrators can toggle to
 * enable or disable sign-in without deleting the account.
 */
public class User {

    private final String id;
    private final String username;
    private String passwordHash;
    private String email;
    private String displayName;
    private Role role;
    private boolean active;
    private final Instant createdAt;

    /**
     * Creates a user.
     *
     * @param id           unique identifier of the user
     * @param username     unique login name of the user
     * @param passwordHash salted hash of the user's password (never the raw password)
     * @param email        contact email of the user
     * @param displayName  human-friendly name shown in the UI
     * @param role         the user's authorization role
     */
    public User(String id, String username, String passwordHash, String email, String displayName, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();
    }

    /** @return the user's unique identifier. */
    public String getId() {
        return id;
    }

    /** @return the user's unique login name. */
    public String getUsername() {
        return username;
    }

    /** @return the salted hash of the user's password. */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Replaces the stored password hash (used when the user changes password).
     *
     * @param passwordHash the new salted password hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** @return the user's contact email. */
    public String getEmail() {
        return email;
    }

    /**
     * Updates the user's contact email as part of profile management.
     *
     * @param email the new email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the user's display name. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Updates the user's display name as part of profile management.
     *
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** @return the user's authorization role. */
    public Role getRole() {
        return role;
    }

    /**
     * Changes the user's authorization role (an administrative operation).
     *
     * @param role the new role
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /** @return {@code true} if the account is allowed to sign in. */
    public boolean isActive() {
        return active;
    }

    /**
     * Enables or disables the account (an administrative operation).
     *
     * @param active {@code true} to allow sign-in, {@code false} to block it
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return {@code true} if the user has the {@link Role#ADMIN} role. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** @return the instant the account was created. */
    public Instant getCreatedAt() {
        return createdAt;
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

    /** @return a short {@code username(role)} representation of the user. */
    @Override
    public String toString() {
        return username + "(" + role + (active ? "" : ", inactive") + ")";
    }
}
