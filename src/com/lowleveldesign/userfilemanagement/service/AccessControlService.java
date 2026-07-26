package com.lowleveldesign.userfilemanagement.service;

import com.lowleveldesign.userfilemanagement.model.File;
import com.lowleveldesign.userfilemanagement.model.Permission;
import com.lowleveldesign.userfilemanagement.model.User;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central authorization component that answers the question "may this user
 * perform this action on this file?" and maintains the per-file grant table
 * that backs those decisions.
 *
 * <p>The rules are uniform:
 * <ul>
 *     <li>An administrator implicitly has every {@link Permission} on every file.</li>
 *     <li>A file's owner implicitly has every permission on that file.</li>
 *     <li>Any other user has only the permissions explicitly granted to them.</li>
 * </ul>
 *
 * <p>Grants are stored as {@code fileId -> (userId -> EnumSet<Permission>)} in
 * thread-safe maps so the access check and the mutation methods can be called
 * concurrently.
 */
public class AccessControlService {

    private final Map<String, Map<String, EnumSet<Permission>>> grants = new ConcurrentHashMap<>();

    /**
     * Tests whether the given user holds a permission on the given file.
     *
     * @param user       the acting user
     * @param file       the target file
     * @param permission the capability being tested
     * @return {@code true} if the user is an admin, the file owner, or has been
     *         explicitly granted {@code permission}
     */
    public boolean hasPermission(User user, File file, Permission permission) {
        if (user.isAdmin() || file.getOwnerId().equals(user.getId())) {
            return true;
        }
        Map<String, EnumSet<Permission>> fileGrants = grants.get(file.getId());
        if (fileGrants == null) {
            return false;
        }
        EnumSet<Permission> userGrants = fileGrants.get(user.getId());
        return userGrants != null && userGrants.contains(permission);
    }

    /**
     * Grants a permission to a user on a file.
     *
     * @param fileId       the file the grant applies to
     * @param targetUserId the user receiving the permission
     * @param permission   the capability being granted
     */
    public void grant(String fileId, String targetUserId, Permission permission) {
        grants.computeIfAbsent(fileId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(targetUserId, k -> EnumSet.noneOf(Permission.class))
                .add(permission);
    }

    /**
     * Revokes a single permission from a user on a file. Grants for other users
     * or other permissions are left untouched.
     *
     * @param fileId       the file the grant applies to
     * @param targetUserId the user losing the permission
     * @param permission   the capability being revoked
     */
    public void revoke(String fileId, String targetUserId, Permission permission) {
        Map<String, EnumSet<Permission>> fileGrants = grants.get(fileId);
        if (fileGrants == null) {
            return;
        }
        EnumSet<Permission> userGrants = fileGrants.get(targetUserId);
        if (userGrants != null) {
            userGrants.remove(permission);
            if (userGrants.isEmpty()) {
                fileGrants.remove(targetUserId);
            }
        }
        if (fileGrants.isEmpty()) {
            grants.remove(fileId);
        }
    }

    /**
     * Removes every grant associated with a file. Called when the file is
     * deleted so no stale permissions linger.
     *
     * @param fileId the file whose grants should be cleared
     */
    public void removeAllForFile(String fileId) {
        grants.remove(fileId);
    }

    /**
     * Returns a read-only snapshot of the explicit grants on a file, excluding
     * the implicit owner and administrator rights.
     *
     * @param fileId the file to inspect
     * @return a map of {@code userId -> permissions}; empty if there are none
     */
    public Map<String, Set<Permission>> getGrants(String fileId) {
        Map<String, EnumSet<Permission>> fileGrants = grants.get(fileId);
        if (fileGrants == null) {
            return Collections.emptyMap();
        }
        Map<String, Set<Permission>> snapshot = new HashMap<>();
        fileGrants.forEach((userId, perms) -> snapshot.put(userId, EnumSet.copyOf(perms)));
        return Collections.unmodifiableMap(snapshot);
    }
}
