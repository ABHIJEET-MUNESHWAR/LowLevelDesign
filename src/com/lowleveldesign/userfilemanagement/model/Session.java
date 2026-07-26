package com.lowleveldesign.userfilemanagement.model;

import java.time.Instant;

/**
 * An authenticated session issued to a {@link User} after a successful sign-in.
 *
 * <p>The opaque {@code token} is presented by the client on every subsequent
 * request so the system can identify the acting user without re-sending
 * credentials. A session records the id of the user it belongs to and the
 * instant it was created.
 */
public class Session {

    private final String token;
    private final String userId;
    private final Instant createdAt;

    /**
     * Creates a session.
     *
     * @param token  the opaque session token presented on subsequent requests
     * @param userId the id of the authenticated user
     */
    public Session(String token, String userId) {
        this.token = token;
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    /** @return the opaque session token. */
    public String getToken() {
        return token;
    }

    /** @return the id of the authenticated user. */
    public String getUserId() {
        return userId;
    }

    /** @return the instant the session was created. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return a short representation that never leaks the full token. */
    @Override
    public String toString() {
        return "Session{user=" + userId + "}";
    }
}
