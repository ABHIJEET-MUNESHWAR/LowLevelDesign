package com.lowleveldesign.userfilemanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Stateless helper that hashes and verifies passwords using a per-password
 * random salt and SHA-256.
 *
 * <p>Raw passwords are never stored: {@link #hash(String)} returns a
 * {@code salt:digest} string (both Base64-encoded) that is safe to persist, and
 * {@link #verify(String, String)} re-derives the digest from a candidate
 * password using the stored salt and compares it in constant time.
 *
 * <p>This is intentionally simple for a machine-coding exercise; a production
 * system would use a slow, memory-hard function such as bcrypt, scrypt or
 * Argon2 instead of a single SHA-256 pass.
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String ALGORITHM = "SHA-256";

    private PasswordHasher() {
    }

    /**
     * Hashes a raw password with a freshly generated random salt.
     *
     * @param rawPassword the plain-text password to hash
     * @return a {@code salt:digest} string, both parts Base64-encoded
     */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] digest = digest(salt, rawPassword);
        return encode(salt) + ":" + encode(digest);
    }

    /**
     * Verifies a candidate password against a previously stored hash.
     *
     * @param rawPassword the candidate plain-text password
     * @param storedHash  the {@code salt:digest} string produced by {@link #hash(String)}
     * @return {@code true} if the candidate password matches, {@code false} otherwise
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        int separator = storedHash.indexOf(':');
        if (separator < 0) {
            return false;
        }
        byte[] salt = decode(storedHash.substring(0, separator));
        byte[] expected = decode(storedHash.substring(separator + 1));
        byte[] actual = digest(salt, rawPassword);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] digest(byte[] salt, String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            md.update(rawPassword.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
