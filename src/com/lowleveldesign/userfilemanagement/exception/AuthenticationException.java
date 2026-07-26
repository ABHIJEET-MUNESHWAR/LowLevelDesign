package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when authentication fails: bad credentials, an unknown username, a
 * disabled account, or an invalid or expired session token.
 */
public class AuthenticationException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of why authentication failed
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
