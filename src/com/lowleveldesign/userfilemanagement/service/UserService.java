package com.lowleveldesign.userfilemanagement.service;

import com.lowleveldesign.userfilemanagement.exception.AccessDeniedException;
import com.lowleveldesign.userfilemanagement.exception.AuthenticationException;
import com.lowleveldesign.userfilemanagement.exception.InvalidInputException;
import com.lowleveldesign.userfilemanagement.exception.UserAlreadyExistsException;
import com.lowleveldesign.userfilemanagement.exception.UserNotFoundException;
import com.lowleveldesign.userfilemanagement.model.Role;
import com.lowleveldesign.userfilemanagement.model.Session;
import com.lowleveldesign.userfilemanagement.model.User;
import com.lowleveldesign.userfilemanagement.util.PasswordHasher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the user lifecycle: registration, authentication (session issuing and
 * validation), self-service profile management, and the administrative
 * operations that manage other accounts.
 *
 * <p>The service stores only salted password hashes (via {@link PasswordHasher})
 * and hands out opaque {@link Session} tokens on successful sign-in. All state
 * is held in memory in thread-safe maps so the class can be exercised from
 * concurrent tests.
 */
public class UserService {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionsByToken = new ConcurrentHashMap<>();

    /**
     * Registers a new ordinary ({@link Role#USER}) account.
     *
     * @param username    unique login name
     * @param password    raw password (stored only as a salted hash)
     * @param email       contact email
     * @param displayName human-friendly display name
     * @return the newly created user
     * @throws InvalidInputException      if any required field is blank
     * @throws UserAlreadyExistsException if the username is already taken
     */
    public User register(String username, String password, String email, String displayName) {
        return createInternal(username, password, email, displayName, Role.USER);
    }

    /**
     * Creates an account with an explicit role. Restricted to administrators.
     *
     * @param adminSession an active session belonging to an administrator
     * @param username     unique login name
     * @param password     raw password (stored only as a salted hash)
     * @param email        contact email
     * @param displayName  human-friendly display name
     * @param role         the role to assign to the new account
     * @return the newly created user
     * @throws AccessDeniedException      if the caller is not an administrator
     * @throws InvalidInputException      if any required field is blank
     * @throws UserAlreadyExistsException if the username is already taken
     */
    public User createUser(Session adminSession, String username, String password,
                           String email, String displayName, Role role) {
        requireAdmin(adminSession);
        if (role == null) {
            throw new InvalidInputException("Role must not be null");
        }
        return createInternal(username, password, email, displayName, role);
    }

    private User createInternal(String username, String password, String email, String displayName, Role role) {
        requireText(username, "Username");
        requireText(password, "Password");
        requireText(email, "Email");
        requireText(displayName, "Display name");
        String key = username.toLowerCase();
        if (userIdByUsername.containsKey(key)) {
            throw new UserAlreadyExistsException(username);
        }
        User user = new User(UUID.randomUUID().toString(), username,
                PasswordHasher.hash(password), email, displayName, role);
        usersById.put(user.getId(), user);
        userIdByUsername.put(key, user.getId());
        return user;
    }

    /**
     * Authenticates a username/password pair and, on success, issues a session.
     *
     * @param username the login name
     * @param password the raw password to verify
     * @return a new {@link Session} for the authenticated user
     * @throws AuthenticationException if the username is unknown, the account is
     *                                 disabled, or the password does not match
     */
    public Session authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new AuthenticationException("Username and password are required");
        }
        String userId = userIdByUsername.get(username.toLowerCase());
        User user = userId == null ? null : usersById.get(userId);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("Account is disabled: " + username);
        }
        Session session = new Session(UUID.randomUUID().toString(), user.getId());
        sessionsByToken.put(session.getToken(), session);
        return session;
    }

    /**
     * Invalidates a session so its token can no longer be used.
     *
     * @param session the session to end (ignored if null or already ended)
     */
    public void logout(Session session) {
        if (session != null) {
            sessionsByToken.remove(session.getToken());
        }
    }

    /**
     * Validates a session token and returns the user it belongs to, enforcing
     * that the session exists and the account is still active.
     *
     * @param session the session presented by the caller
     * @return the authenticated, active user
     * @throws AuthenticationException if the session is null, unknown, or the
     *                                 account has since been disabled
     */
    public User requireActiveUser(Session session) {
        if (session == null || !sessionsByToken.containsKey(session.getToken())) {
            throw new AuthenticationException("Invalid or expired session");
        }
        User user = usersById.get(session.getUserId());
        if (user == null || !user.isActive()) {
            sessionsByToken.remove(session.getToken());
            throw new AuthenticationException("Account is no longer active");
        }
        return user;
    }

    /**
     * Validates that the session belongs to an active administrator.
     *
     * @param session the session presented by the caller
     * @return the administrator user
     * @throws AccessDeniedException if the caller is authenticated but not an admin
     */
    public User requireAdmin(Session session) {
        User user = requireActiveUser(session);
        if (!user.isAdmin()) {
            throw new AccessDeniedException("Administrator privileges are required");
        }
        return user;
    }

    /**
     * Looks up a user by id.
     *
     * @param userId the id to resolve
     * @return the matching user
     * @throws UserNotFoundException if no user has that id
     */
    public User getUserById(String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    /**
     * Updates the caller's own profile fields. Null arguments leave the
     * corresponding field unchanged.
     *
     * @param session     the caller's session
     * @param email       new email, or null to keep the current one
     * @param displayName new display name, or null to keep the current one
     * @return the updated user
     */
    public User updateProfile(Session session, String email, String displayName) {
        User user = requireActiveUser(session);
        if (email != null) {
            requireText(email, "Email");
            user.setEmail(email);
        }
        if (displayName != null) {
            requireText(displayName, "Display name");
            user.setDisplayName(displayName);
        }
        return user;
    }

    /**
     * Changes the caller's own password after verifying the current one.
     *
     * @param session         the caller's session
     * @param currentPassword the caller's existing password
     * @param newPassword     the desired new password
     * @throws AuthenticationException if the current password is incorrect
     * @throws InvalidInputException   if the new password is blank
     */
    public void changePassword(Session session, String currentPassword, String newPassword) {
        User user = requireActiveUser(session);
        if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        requireText(newPassword, "New password");
        user.setPasswordHash(PasswordHasher.hash(newPassword));
    }

    /**
     * Enables or disables a target account. Restricted to administrators.
     *
     * @param adminSession an active administrator session
     * @param targetUserId the id of the account to toggle
     * @param active       {@code true} to enable sign-in, {@code false} to block it
     * @return the updated target user
     */
    public User setUserActive(Session adminSession, String targetUserId, boolean active) {
        requireAdmin(adminSession);
        User target = getUserById(targetUserId);
        target.setActive(active);
        if (!active) {
            invalidateSessionsFor(targetUserId);
        }
        return target;
    }

    /**
     * Changes a target account's role. Restricted to administrators.
     *
     * @param adminSession an active administrator session
     * @param targetUserId the id of the account to modify
     * @param role         the new role
     * @return the updated target user
     */
    public User changeRole(Session adminSession, String targetUserId, Role role) {
        requireAdmin(adminSession);
        if (role == null) {
            throw new InvalidInputException("Role must not be null");
        }
        User target = getUserById(targetUserId);
        target.setRole(role);
        return target;
    }

    /**
     * Lists every registered user. Restricted to administrators.
     *
     * @param adminSession an active administrator session
     * @return a snapshot list of all users
     */
    public List<User> listUsers(Session adminSession) {
        requireAdmin(adminSession);
        return new ArrayList<>(usersById.values());
    }

    private void invalidateSessionsFor(String userId) {
        sessionsByToken.values().removeIf(s -> s.getUserId().equals(userId));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field + " must not be blank");
        }
    }
}
