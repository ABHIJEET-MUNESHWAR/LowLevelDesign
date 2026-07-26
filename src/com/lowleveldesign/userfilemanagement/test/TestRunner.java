package com.lowleveldesign.userfilemanagement.test;

import com.lowleveldesign.userfilemanagement.exception.AccessDeniedException;
import com.lowleveldesign.userfilemanagement.exception.AuthenticationException;
import com.lowleveldesign.userfilemanagement.exception.FileAlreadyExistsException;
import com.lowleveldesign.userfilemanagement.exception.FileNotFoundException;
import com.lowleveldesign.userfilemanagement.exception.InvalidInputException;
import com.lowleveldesign.userfilemanagement.exception.UserAlreadyExistsException;
import com.lowleveldesign.userfilemanagement.model.File;
import com.lowleveldesign.userfilemanagement.model.Permission;
import com.lowleveldesign.userfilemanagement.model.Role;
import com.lowleveldesign.userfilemanagement.model.Session;
import com.lowleveldesign.userfilemanagement.model.User;
import com.lowleveldesign.userfilemanagement.service.FileManagementSystem;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free correctness and concurrency test suite (no JUnit required). Run via
 * {@code java com.lowleveldesign.userfilemanagement.test.TestRunner}; exits non-zero on failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Runs every test in the suite and prints a pass/fail line per test plus a final tally.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        run("registration then authentication issues a usable session", TestRunner::testRegisterAndLogin);
        run("duplicate username is rejected (case-insensitive)", TestRunner::testDuplicateUsername);
        run("blank credentials are rejected at registration", TestRunner::testBlankInputRejected);
        run("wrong password and unknown user fail authentication", TestRunner::testBadCredentials);
        run("profile update and password change take effect", TestRunner::testProfileAndPasswordChange);
        run("owner has full CRUD on their own file", TestRunner::testOwnerCrud);
        run("file names are unique per owner but not across owners", TestRunner::testUniqueNamePerOwner);
        run("non-owner is denied until granted, per-permission", TestRunner::testPermissionGrants);
        run("revoking a permission removes access", TestRunner::testRevoke);
        run("admin can access and manage any file", TestRunner::testAdminOverride);
        run("non-admin cannot perform admin operations", TestRunner::testAdminGuard);
        run("deactivating a user blocks login and invalidates sessions", TestRunner::testDeactivation);
        run("deleting a file clears its grants and it is gone", TestRunner::testDeleteClearsGrants);
        run("listing reflects ownership, grants and admin scope", TestRunner::testListing);
        run("invalid or expired session is rejected", TestRunner::testInvalidSession);
        run("concurrent create of same-named file yields exactly one", TestRunner::testConcurrentCreate);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Executes a single test, records the outcome, and prints its result line.
     *
     * @param name the human-readable test description to print
     * @param test the test body; returning normally means pass, throwing means fail
     */
    private static void run(String name, Callable<Void> test) {
        try {
            test.call();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " -- " + t.getMessage());
        }
    }

    /**
     * Fails the current test if {@code condition} is false.
     *
     * @param condition the condition that must hold
     * @param message   the failure description reported when it does not
     */
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Fails the current test if {@code actual} does not equal {@code expected}.
     *
     * @param expected the value the test requires
     * @param actual   the value produced by the code under test
     * @param message  the failure description
     */
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /**
     * Fails the current test if {@code action} does not throw an exception of the expected type.
     *
     * @param expected the exception type that must be thrown
     * @param action   the code expected to fail
     * @param message  the failure description
     */
    private static void assertThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " (wrong type: " + t.getClass().getSimpleName() + ")");
        }
        throw new AssertionError(message + " (nothing thrown)");
    }

    /**
     * Builds a fresh system with a seeded administrator whose session is returned via the array.
     *
     * @param outAdminSession a one-element array that receives the admin session
     * @return a ready-to-use system
     */
    private static FileManagementSystem newSystemWithAdmin(Session[] outAdminSession) {
        FileManagementSystem system = new FileManagementSystem();
        User root = system.register("root", "root-pass", "root@mail.com", "Root");
        root.setRole(Role.ADMIN);
        outAdminSession[0] = system.login("root", "root-pass");
        return system;
    }

    private static Void testRegisterAndLogin() {
        FileManagementSystem system = new FileManagementSystem();
        User alice = system.register("alice", "pw", "alice@mail.com", "Alice");
        assertEquals("alice", alice.getUsername(), "username should round-trip");
        assertEquals(Role.USER, alice.getRole(), "self-registration should yield a USER");
        Session session = system.login("alice", "pw");
        assertTrue(session != null && session.getToken() != null, "login should issue a session");
        // The session must actually authorize an operation.
        File f = system.createFile(session, "a.txt", "hi");
        assertEquals("hi", system.readFile(session, f.getId()).getContent(), "session should authorize CRUD");
        return null;
    }

    private static Void testDuplicateUsername() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "alice@mail.com", "Alice");
        assertThrows(UserAlreadyExistsException.class,
                () -> system.register("ALICE", "pw2", "a2@mail.com", "Alice2"),
                "duplicate username (case-insensitive) should be rejected");
        return null;
    }

    private static Void testBlankInputRejected() {
        FileManagementSystem system = new FileManagementSystem();
        assertThrows(InvalidInputException.class,
                () -> system.register(" ", "pw", "e@mail.com", "X"), "blank username should be rejected");
        assertThrows(InvalidInputException.class,
                () -> system.register("bob", "", "e@mail.com", "X"), "blank password should be rejected");
        return null;
    }

    private static Void testBadCredentials() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "alice@mail.com", "Alice");
        assertThrows(AuthenticationException.class,
                () -> system.login("alice", "wrong"), "wrong password should fail");
        assertThrows(AuthenticationException.class,
                () -> system.login("ghost", "pw"), "unknown user should fail");
        return null;
    }

    private static Void testProfileAndPasswordChange() {
        FileManagementSystem system = new FileManagementSystem();
        User alice = system.register("alice", "pw", "alice@mail.com", "Alice");
        Session s = system.login("alice", "pw");
        system.updateProfile(s, "new@mail.com", "Ali");
        assertEquals("new@mail.com", alice.getEmail(), "email should update");
        assertEquals("Ali", alice.getDisplayName(), "display name should update");
        assertThrows(AuthenticationException.class,
                () -> system.changePassword(s, "wrong", "np"), "wrong current password should fail");
        system.changePassword(s, "pw", "newpw");
        assertThrows(AuthenticationException.class,
                () -> system.login("alice", "pw"), "old password should no longer work");
        assertTrue(system.login("alice", "newpw") != null, "new password should work");
        return null;
    }

    private static Void testOwnerCrud() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "alice@mail.com", "Alice");
        Session s = system.login("alice", "pw");
        File f = system.createFile(s, "doc.txt", "v1");
        assertEquals("v1", system.readFile(s, f.getId()).getContent(), "read should return content");
        system.updateFile(s, f.getId(), "v2");
        assertEquals("v2", system.readFile(s, f.getId()).getContent(), "update should persist");
        system.renameFile(s, f.getId(), "doc2.txt");
        assertEquals("doc2.txt", system.readFile(s, f.getId()).getName(), "rename should persist");
        system.deleteFile(s, f.getId());
        assertThrows(FileNotFoundException.class,
                () -> system.readFile(s, f.getId()), "deleted file should be gone");
        return null;
    }

    private static Void testUniqueNamePerOwner() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "a@mail.com", "Alice");
        system.register("bob", "pw", "b@mail.com", "Bob");
        Session as = system.login("alice", "pw");
        Session bs = system.login("bob", "pw");
        system.createFile(as, "shared-name.txt", "a");
        assertThrows(FileAlreadyExistsException.class,
                () -> system.createFile(as, "shared-name.txt", "dup"),
                "same owner cannot reuse a file name");
        // Different owner may use the same name.
        assertTrue(system.createFile(bs, "shared-name.txt", "b") != null,
                "different owners may share a file name");
        return null;
    }

    private static Void testPermissionGrants() {
        FileManagementSystem system = new FileManagementSystem();
        User alice = system.register("alice", "pw", "a@mail.com", "Alice");
        User bob = system.register("bob", "pw", "b@mail.com", "Bob");
        Session as = system.login("alice", "pw");
        Session bs = system.login("bob", "pw");
        File f = system.createFile(as, "notes.txt", "secret");

        assertThrows(AccessDeniedException.class,
                () -> system.readFile(bs, f.getId()), "non-owner cannot read without grant");

        system.grantPermission(as, f.getId(), bob.getId(), Permission.READ);
        assertEquals("secret", system.readFile(bs, f.getId()).getContent(), "READ grant enables reading");
        assertThrows(AccessDeniedException.class,
                () -> system.updateFile(bs, f.getId(), "x"), "READ grant must not enable writing");

        system.grantPermission(as, f.getId(), bob.getId(), Permission.WRITE);
        system.updateFile(bs, f.getId(), "edited");
        assertEquals("edited", system.readFile(as, f.getId()).getContent(), "WRITE grant enables writing");

        // Bob cannot share the file (no SHARE permission) even though he can write.
        assertThrows(AccessDeniedException.class,
                () -> system.grantPermission(bs, f.getId(), alice.getId(), Permission.READ),
                "WRITE does not imply SHARE");
        // Sanity: alice is unused-warning-free reference.
        assertTrue(alice != null, "owner reference exists");
        return null;
    }

    private static Void testRevoke() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "a@mail.com", "Alice");
        User bob = system.register("bob", "pw", "b@mail.com", "Bob");
        Session as = system.login("alice", "pw");
        Session bs = system.login("bob", "pw");
        File f = system.createFile(as, "notes.txt", "data");
        system.grantPermission(as, f.getId(), bob.getId(), Permission.READ);
        assertTrue(system.readFile(bs, f.getId()) != null, "grant should allow read");
        system.revokePermission(as, f.getId(), bob.getId(), Permission.READ);
        assertThrows(AccessDeniedException.class,
                () -> system.readFile(bs, f.getId()), "revoke should remove access");
        return null;
    }

    private static Void testAdminOverride() {
        Session[] admin = new Session[1];
        FileManagementSystem system = newSystemWithAdmin(admin);
        system.register("alice", "pw", "a@mail.com", "Alice");
        Session as = system.login("alice", "pw");
        File f = system.createFile(as, "private.txt", "top-secret");
        // Admin can read, write and delete a file it does not own.
        assertEquals("top-secret", system.readFile(admin[0], f.getId()).getContent(), "admin can read any file");
        system.updateFile(admin[0], f.getId(), "changed by admin");
        assertEquals("changed by admin", system.readFile(as, f.getId()).getContent(), "admin can write any file");
        system.deleteFile(admin[0], f.getId());
        assertThrows(FileNotFoundException.class,
                () -> system.readFile(as, f.getId()), "admin can delete any file");
        return null;
    }

    private static Void testAdminGuard() {
        Session[] admin = new Session[1];
        FileManagementSystem system = newSystemWithAdmin(admin);
        User alice = system.register("alice", "pw", "a@mail.com", "Alice");
        Session as = system.login("alice", "pw");
        assertThrows(AccessDeniedException.class,
                () -> system.listUsers(as), "non-admin cannot list users");
        assertThrows(AccessDeniedException.class,
                () -> system.createUser(as, "x", "pw", "x@mail.com", "X", Role.USER),
                "non-admin cannot create users");
        assertThrows(AccessDeniedException.class,
                () -> system.changeRole(as, alice.getId(), Role.ADMIN),
                "non-admin cannot change roles");
        return null;
    }

    private static Void testDeactivation() {
        Session[] admin = new Session[1];
        FileManagementSystem system = newSystemWithAdmin(admin);
        User bob = system.register("bob", "pw", "b@mail.com", "Bob");
        Session bs = system.login("bob", "pw");
        system.setUserActive(admin[0], bob.getId(), false);
        // Existing session must stop working.
        assertThrows(AuthenticationException.class,
                () -> system.createFile(bs, "x.txt", "y"), "deactivated user's session must be invalid");
        // And new logins must fail.
        assertThrows(AuthenticationException.class,
                () -> system.login("bob", "pw"), "deactivated user cannot log in");
        // Reactivation restores login.
        system.setUserActive(admin[0], bob.getId(), true);
        assertTrue(system.login("bob", "pw") != null, "reactivated user can log in again");
        return null;
    }

    private static Void testDeleteClearsGrants() {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "a@mail.com", "Alice");
        User bob = system.register("bob", "pw", "b@mail.com", "Bob");
        Session as = system.login("alice", "pw");
        Session bs = system.login("bob", "pw");
        File f = system.createFile(as, "notes.txt", "data");
        system.grantPermission(as, f.getId(), bob.getId(), Permission.READ);
        system.deleteFile(as, f.getId());
        // Re-creating a file with the same name gets a new id; the old grant must not leak.
        File f2 = system.createFile(as, "notes.txt", "fresh");
        assertThrows(AccessDeniedException.class,
                () -> system.readFile(bs, f2.getId()), "stale grants must not carry over to a new file");
        return null;
    }

    private static Void testListing() {
        Session[] admin = new Session[1];
        FileManagementSystem system = newSystemWithAdmin(admin);
        User alice = system.register("alice", "pw", "a@mail.com", "Alice");
        User bob = system.register("bob", "pw", "b@mail.com", "Bob");
        Session as = system.login("alice", "pw");
        Session bs = system.login("bob", "pw");
        File a1 = system.createFile(as, "a1.txt", "x");
        system.createFile(as, "a2.txt", "y");
        File b1 = system.createFile(bs, "b1.txt", "z");

        assertEquals(2, system.listFiles(as).size(), "alice sees her two files");
        // Share one of alice's files with bob -> bob now sees his own + shared.
        system.grantPermission(as, a1.getId(), bob.getId(), Permission.READ);
        assertEquals(2, system.listFiles(bs).size(), "bob sees his file plus the shared one");
        // Admin sees everything (root + a1 + a2 + b1 = 3 files; root created none).
        assertEquals(3, system.listFiles(admin[0]).size(), "admin sees all files");
        assertTrue(system.listUsers(admin[0]).size() == 3, "admin lists root, alice, bob");
        assertTrue(b1 != null && alice != null, "references exist");
        return null;
    }

    private static Void testInvalidSession() {
        FileManagementSystem system = new FileManagementSystem();
        Session bogus = new Session("not-a-real-token", "nobody");
        assertThrows(AuthenticationException.class,
                () -> system.createFile(bogus, "x.txt", "y"), "bogus session should be rejected");
        // Logout should invalidate a real session too.
        system.register("alice", "pw", "a@mail.com", "Alice");
        Session s = system.login("alice", "pw");
        system.logout(s);
        assertThrows(AuthenticationException.class,
                () -> system.createFile(s, "x.txt", "y"), "logged-out session should be rejected");
        return null;
    }

    private static Void testConcurrentCreate() throws Exception {
        FileManagementSystem system = new FileManagementSystem();
        system.register("alice", "pw", "a@mail.com", "Alice");
        Session s = system.login("alice", "pw");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger created = new AtomicInteger();
        java.util.List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    system.createFile(s, "race.txt", "content");
                    created.incrementAndGet();
                } catch (FileAlreadyExistsException ignored) {
                    // expected for all but the winner
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long owned = system.listFiles(s).stream().filter(f -> f.getName().equals("race.txt")).count();
        assertEquals(1L, owned, "exactly one file named race.txt should exist for the owner");
        assertTrue(created.get() >= 1, "at least one creation should have succeeded");
        return null;
    }
}
