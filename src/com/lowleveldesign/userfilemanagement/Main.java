package com.lowleveldesign.userfilemanagement;

import com.lowleveldesign.userfilemanagement.exception.AccessDeniedException;
import com.lowleveldesign.userfilemanagement.model.File;
import com.lowleveldesign.userfilemanagement.model.Permission;
import com.lowleveldesign.userfilemanagement.model.Role;
import com.lowleveldesign.userfilemanagement.model.Session;
import com.lowleveldesign.userfilemanagement.model.User;
import com.lowleveldesign.userfilemanagement.service.FileManagementSystem;

/**
 * Demonstrates the User and File Management System end to end: bootstrapping an
 * administrator, registering users, performing file CRUD, sharing files with
 * scoped permissions, enforcing access control, and administering accounts.
 */
public class Main {

    /**
     * Runs the demonstration scenario.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        FileManagementSystem system = new FileManagementSystem();

        System.out.println("==== Bootstrap: seed the first administrator ====");
        User rootAdmin = seedAdmin(system);
        Session adminSession = system.login("root", "root-pass");
        System.out.println("Admin online: " + rootAdmin);

        System.out.println("\n==== User registration ====");
        User alice = system.register("alice", "alice-pass", "alice@mail.com", "Alice A.");
        User bob = system.register("bob", "bob-pass", "bob@mail.com", "Bob B.");
        System.out.println("Registered: " + alice + ", " + bob);

        Session aliceSession = system.login("alice", "alice-pass");
        Session bobSession = system.login("bob", "bob-pass");

        System.out.println("\n==== Profile management ====");
        system.updateProfile(aliceSession, "alice@work.com", null);
        system.changePassword(aliceSession, "alice-pass", "alice-secret");
        System.out.println("Alice email is now " + alice.getEmail() + "; password changed.");

        System.out.println("\n==== File CRUD (Alice owns files) ====");
        File notes = system.createFile(aliceSession, "notes.txt", "first line");
        System.out.println("Created " + notes + " with content: '" + notes.getContent() + "'");
        system.updateFile(aliceSession, notes.getId(), "first line\nsecond line");
        System.out.println("After update: '" + system.readFile(aliceSession, notes.getId()).getContent() + "'");

        System.out.println("\n==== Access control: Bob cannot touch Alice's file ====");
        try {
            system.readFile(bobSession, notes.getId());
        } catch (AccessDeniedException e) {
            System.out.println("Denied as expected: " + e.getMessage());
        }

        System.out.println("\n==== Sharing: Alice grants Bob READ ====");
        system.grantPermission(aliceSession, notes.getId(), bob.getId(), Permission.READ);
        System.out.println("Bob can now read: '" + system.readFile(bobSession, notes.getId()).getContent() + "'");
        try {
            system.updateFile(bobSession, notes.getId(), "hacked");
        } catch (AccessDeniedException e) {
            System.out.println("Bob still cannot WRITE: " + e.getMessage());
        }

        System.out.println("\n==== Sharing: Alice grants Bob WRITE, then revokes READ ====");
        system.grantPermission(aliceSession, notes.getId(), bob.getId(), Permission.WRITE);
        system.updateFile(bobSession, notes.getId(), "edited by bob");
        System.out.println("Bob edited content to: '" + system.readFile(aliceSession, notes.getId()).getContent() + "'");
        system.revokePermission(aliceSession, notes.getId(), bob.getId(), Permission.READ);
        try {
            system.readFile(bobSession, notes.getId());
        } catch (AccessDeniedException e) {
            System.out.println("READ revoked, Bob blocked again: " + e.getMessage());
        }

        System.out.println("\n==== Admin overrides: admin can read and manage any file ====");
        System.out.println("Admin reads Alice's file: '" + system.readFile(adminSession, notes.getId()).getContent() + "'");

        System.out.println("\n==== Admin account management ====");
        User carol = system.createUser(adminSession, "carol", "carol-pass", "carol@mail.com", "Carol C.", Role.ADMIN);
        System.out.println("Admin created another admin: " + carol);
        system.setUserActive(adminSession, bob.getId(), false);
        System.out.println("Bob deactivated; his session is now invalid.");
        try {
            system.login("bob", "bob-pass");
        } catch (RuntimeException e) {
            System.out.println("Bob cannot log in: " + e.getMessage());
        }

        System.out.println("\n==== Listing (admin sees everyone; Alice sees her files) ====");
        System.out.println("All users: " + system.listUsers(adminSession));
        System.out.println("Alice's accessible files: " + system.listFiles(aliceSession));

        System.out.println("\n==== Delete ====");
        system.deleteFile(aliceSession, notes.getId());
        System.out.println("Alice deleted the file. Remaining accessible files: " + system.listFiles(aliceSession));
    }

    /**
     * Seeds the first administrator. Registering the very first admin is an
     * inherent chicken-and-egg problem (only an admin can create an admin), so
     * the initial account is registered through the normal API and then elevated
     * once on the domain object. Every subsequent admin is created purely through
     * the public, permission-checked {@code createUser}/{@code changeRole} API.
     *
     * @param system the system to seed
     * @return the seeded root administrator
     */
    private static User seedAdmin(FileManagementSystem system) {
        User root = system.register("root", "root-pass", "root@mail.com", "Root Admin");
        root.setRole(Role.ADMIN);
        return root;
    }
}
