package com.lowleveldesign.userfilemanagement.service;

import com.lowleveldesign.userfilemanagement.model.File;
import com.lowleveldesign.userfilemanagement.model.Permission;
import com.lowleveldesign.userfilemanagement.model.Role;
import com.lowleveldesign.userfilemanagement.model.Session;
import com.lowleveldesign.userfilemanagement.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade that wires together {@link UserService}, {@link AccessControlService}
 * and {@link FileService} into a single entry point for client code.
 *
 * <p>It exposes the whole feature set - user management, authentication, file
 * CRUD and sharing - through one cohesive API while delegating each call to the
 * appropriate collaborator, so callers never have to assemble the object graph
 * themselves.
 */
public class FileManagementSystem {

    private final UserService userService;
    private final FileService fileService;
    private final AccessControlService accessControl;

    /**
     * Builds the system with freshly constructed, wired-together collaborators.
     */
    public FileManagementSystem() {
        this.userService = new UserService();
        this.accessControl = new AccessControlService();
        this.fileService = new FileService(userService, accessControl);
    }

    // ---------------------------------------------------------------------
    // User management
    // ---------------------------------------------------------------------

    /**
     * Registers a new ordinary user.
     *
     * @param username    unique login name
     * @param password    raw password
     * @param email       contact email
     * @param displayName display name
     * @return the newly created user
     */
    public User register(String username, String password, String email, String displayName) {
        return userService.register(username, password, email, displayName);
    }

    /**
     * Creates an account with an explicit role (administrators only).
     *
     * @param adminSession an active administrator session
     * @param username     unique login name
     * @param password     raw password
     * @param email        contact email
     * @param displayName  display name
     * @param role         the role to assign
     * @return the newly created user
     */
    public User createUser(Session adminSession, String username, String password,
                           String email, String displayName, Role role) {
        return userService.createUser(adminSession, username, password, email, displayName, role);
    }

    /**
     * Authenticates a user and issues a session.
     *
     * @param username the login name
     * @param password the raw password
     * @return a session for the authenticated user
     */
    public Session login(String username, String password) {
        return userService.authenticate(username, password);
    }

    /**
     * Ends a session.
     *
     * @param session the session to invalidate
     */
    public void logout(Session session) {
        userService.logout(session);
    }

    /**
     * Updates the caller's own profile.
     *
     * @param session     the caller's session
     * @param email       new email, or null to leave unchanged
     * @param displayName new display name, or null to leave unchanged
     * @return the updated user
     */
    public User updateProfile(Session session, String email, String displayName) {
        return userService.updateProfile(session, email, displayName);
    }

    /**
     * Changes the caller's own password.
     *
     * @param session         the caller's session
     * @param currentPassword the existing password
     * @param newPassword     the new password
     */
    public void changePassword(Session session, String currentPassword, String newPassword) {
        userService.changePassword(session, currentPassword, newPassword);
    }

    /**
     * Enables or disables an account (administrators only).
     *
     * @param adminSession an active administrator session
     * @param targetUserId the account to toggle
     * @param active       whether the account may sign in
     * @return the updated user
     */
    public User setUserActive(Session adminSession, String targetUserId, boolean active) {
        return userService.setUserActive(adminSession, targetUserId, active);
    }

    /**
     * Changes an account's role (administrators only).
     *
     * @param adminSession an active administrator session
     * @param targetUserId the account to modify
     * @param role         the new role
     * @return the updated user
     */
    public User changeRole(Session adminSession, String targetUserId, Role role) {
        return userService.changeRole(adminSession, targetUserId, role);
    }

    /**
     * Lists all users (administrators only).
     *
     * @param adminSession an active administrator session
     * @return all registered users
     */
    public List<User> listUsers(Session adminSession) {
        return userService.listUsers(adminSession);
    }

    // ---------------------------------------------------------------------
    // File management
    // ---------------------------------------------------------------------

    /**
     * Creates a file owned by the caller.
     *
     * @param session the caller's session
     * @param name    the file name
     * @param content the initial content
     * @return the created file
     */
    public File createFile(Session session, String name, String content) {
        return fileService.createFile(session, name, content);
    }

    /**
     * Reads a file the caller may read.
     *
     * @param session the caller's session
     * @param fileId  the file id
     * @return the file
     */
    public File readFile(Session session, String fileId) {
        return fileService.readFile(session, fileId);
    }

    /**
     * Updates a file's content.
     *
     * @param session the caller's session
     * @param fileId  the file id
     * @param content the new content
     * @return the updated file
     */
    public File updateFile(Session session, String fileId, String content) {
        return fileService.updateFile(session, fileId, content);
    }

    /**
     * Renames a file.
     *
     * @param session the caller's session
     * @param fileId  the file id
     * @param newName the new name
     * @return the updated file
     */
    public File renameFile(Session session, String fileId, String newName) {
        return fileService.renameFile(session, fileId, newName);
    }

    /**
     * Deletes a file.
     *
     * @param session the caller's session
     * @param fileId  the file id
     */
    public void deleteFile(Session session, String fileId) {
        fileService.deleteFile(session, fileId);
    }

    /**
     * Lists files the caller can read.
     *
     * @param session the caller's session
     * @return the accessible files
     */
    public List<File> listFiles(Session session) {
        return fileService.listAccessibleFiles(session);
    }

    // ---------------------------------------------------------------------
    // Sharing / access control
    // ---------------------------------------------------------------------

    /**
     * Grants a permission on a file to another user.
     *
     * @param session      the caller's session
     * @param fileId       the file to share
     * @param targetUserId the recipient
     * @param permission   the capability to grant
     */
    public void grantPermission(Session session, String fileId, String targetUserId, Permission permission) {
        fileService.grantPermission(session, fileId, targetUserId, permission);
    }

    /**
     * Revokes a permission on a file from another user.
     *
     * @param session      the caller's session
     * @param fileId       the file to unshare
     * @param targetUserId the user losing access
     * @param permission   the capability to revoke
     */
    public void revokePermission(Session session, String fileId, String targetUserId, Permission permission) {
        fileService.revokePermission(session, fileId, targetUserId, permission);
    }

    /**
     * Returns the explicit permission grants on a file.
     *
     * @param session the caller's session
     * @param fileId  the file to inspect
     * @return a map of {@code userId -> permissions}
     */
    public Map<String, Set<Permission>> getPermissions(Session session, String fileId) {
        return fileService.getPermissions(session, fileId);
    }
}
