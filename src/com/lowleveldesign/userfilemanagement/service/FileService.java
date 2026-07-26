package com.lowleveldesign.userfilemanagement.service;

import com.lowleveldesign.userfilemanagement.exception.AccessDeniedException;
import com.lowleveldesign.userfilemanagement.exception.FileAlreadyExistsException;
import com.lowleveldesign.userfilemanagement.exception.FileNotFoundException;
import com.lowleveldesign.userfilemanagement.exception.InvalidInputException;
import com.lowleveldesign.userfilemanagement.model.File;
import com.lowleveldesign.userfilemanagement.model.Permission;
import com.lowleveldesign.userfilemanagement.model.Session;
import com.lowleveldesign.userfilemanagement.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the file lifecycle: create, read, update, rename and delete, plus the
 * sharing operations that grant and revoke permissions to other users.
 *
 * <p>Every operation begins by validating the caller's {@link Session} through
 * {@link UserService} and then consults {@link AccessControlService} to enforce
 * that the caller holds the required {@link Permission}. Business rules such as
 * "file names are unique per owner" are enforced here.
 */
public class FileService {

    private final Map<String, File> filesById = new ConcurrentHashMap<>();
    private final Map<String, Boolean> ownerNameKeys = new ConcurrentHashMap<>();
    private final UserService userService;
    private final AccessControlService accessControl;

    /**
     * Creates the service over its collaborators.
     *
     * @param userService   used to authenticate sessions and resolve users
     * @param accessControl used to enforce and mutate file permissions
     */
    public FileService(UserService userService, AccessControlService accessControl) {
        this.userService = userService;
        this.accessControl = accessControl;
    }

    /**
     * Creates a new file owned by the caller.
     *
     * @param session the caller's session
     * @param name    the file name (must be unique among the caller's files)
     * @param content the initial content
     * @return the newly created file
     * @throws InvalidInputException       if the name is blank
     * @throws FileAlreadyExistsException  if the caller already owns a file with that name
     */
    public File createFile(Session session, String name, String content) {
        User user = userService.requireActiveUser(session);
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("File name must not be blank");
        }
        String key = nameKey(user.getId(), name);
        if (ownerNameKeys.putIfAbsent(key, Boolean.TRUE) != null) {
            throw new FileAlreadyExistsException(name);
        }
        File file = new File(UUID.randomUUID().toString(), name, content == null ? "" : content, user.getId());
        filesById.put(file.getId(), file);
        return file;
    }

    /**
     * Reads a file the caller is allowed to read.
     *
     * @param session the caller's session
     * @param fileId  the id of the file to read
     * @return the file
     * @throws FileNotFoundException if no such file exists
     * @throws AccessDeniedException if the caller lacks {@link Permission#READ}
     */
    public File readFile(Session session, String fileId) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.READ);
        return file;
    }

    /**
     * Updates the content of a file the caller is allowed to modify.
     *
     * @param session the caller's session
     * @param fileId  the id of the file to update
     * @param content the new content
     * @return the updated file
     * @throws FileNotFoundException if no such file exists
     * @throws AccessDeniedException if the caller lacks {@link Permission#WRITE}
     */
    public File updateFile(Session session, String fileId, String content) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.WRITE);
        file.setContent(content == null ? "" : content);
        return file;
    }

    /**
     * Renames a file the caller is allowed to modify, keeping names unique per
     * owner.
     *
     * @param session the caller's session
     * @param fileId  the id of the file to rename
     * @param newName the new file name
     * @return the updated file
     * @throws FileNotFoundException      if no such file exists
     * @throws AccessDeniedException      if the caller lacks {@link Permission#WRITE}
     * @throws FileAlreadyExistsException if the owner already has a file with that name
     */
    public File renameFile(Session session, String fileId, String newName) {
        User user = userService.requireActiveUser(session);
        if (newName == null || newName.trim().isEmpty()) {
            throw new InvalidInputException("File name must not be blank");
        }
        File file = requireFile(fileId);
        authorize(user, file, Permission.WRITE);
        if (file.getName().equals(newName)) {
            return file;
        }
        String newKey = nameKey(file.getOwnerId(), newName);
        if (ownerNameKeys.putIfAbsent(newKey, Boolean.TRUE) != null) {
            throw new FileAlreadyExistsException(newName);
        }
        ownerNameKeys.remove(nameKey(file.getOwnerId(), file.getName()));
        file.setName(newName);
        return file;
    }

    /**
     * Deletes a file the caller is allowed to delete, also clearing its grants.
     *
     * @param session the caller's session
     * @param fileId  the id of the file to delete
     * @throws FileNotFoundException if no such file exists
     * @throws AccessDeniedException if the caller lacks {@link Permission#DELETE}
     */
    public void deleteFile(Session session, String fileId) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.DELETE);
        filesById.remove(fileId);
        ownerNameKeys.remove(nameKey(file.getOwnerId(), file.getName()));
        accessControl.removeAllForFile(fileId);
    }

    /**
     * Lists the files the caller can read: an administrator sees all files;
     * anyone else sees files they own or have been granted read access to.
     *
     * @param session the caller's session
     * @return a list of files visible to the caller
     */
    public List<File> listAccessibleFiles(Session session) {
        User user = userService.requireActiveUser(session);
        List<File> visible = new ArrayList<>();
        for (File file : filesById.values()) {
            if (accessControl.hasPermission(user, file, Permission.READ)) {
                visible.add(file);
            }
        }
        return visible;
    }

    /**
     * Grants a permission on a file to another user. The caller must be able to
     * share the file (owner or administrator, i.e. hold {@link Permission#SHARE}).
     *
     * @param session      the caller's session
     * @param fileId       the file to share
     * @param targetUserId the recipient of the permission
     * @param permission   the capability to grant
     * @throws FileNotFoundException if no such file exists
     * @throws AccessDeniedException if the caller lacks {@link Permission#SHARE}
     */
    public void grantPermission(Session session, String fileId, String targetUserId, Permission permission) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.SHARE);
        User target = userService.getUserById(targetUserId);
        if (target.getId().equals(file.getOwnerId())) {
            return;
        }
        accessControl.grant(fileId, target.getId(), permission);
    }

    /**
     * Revokes a permission on a file from another user.
     *
     * @param session      the caller's session
     * @param fileId       the file to unshare
     * @param targetUserId the user losing the permission
     * @param permission   the capability to revoke
     * @throws FileNotFoundException if no such file exists
     * @throws AccessDeniedException if the caller lacks {@link Permission#SHARE}
     */
    public void revokePermission(Session session, String fileId, String targetUserId, Permission permission) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.SHARE);
        accessControl.revoke(fileId, targetUserId, permission);
    }

    /**
     * Returns the explicit permission grants on a file. Visible only to callers
     * who can share the file.
     *
     * @param session the caller's session
     * @param fileId  the file to inspect
     * @return a map of {@code userId -> permissions}
     * @throws AccessDeniedException if the caller lacks {@link Permission#SHARE}
     */
    public Map<String, Set<Permission>> getPermissions(Session session, String fileId) {
        User user = userService.requireActiveUser(session);
        File file = requireFile(fileId);
        authorize(user, file, Permission.SHARE);
        return accessControl.getGrants(fileId);
    }

    private File requireFile(String fileId) {
        File file = filesById.get(fileId);
        if (file == null) {
            throw new FileNotFoundException(fileId);
        }
        return file;
    }

    private static String nameKey(String ownerId, String name) {
        return ownerId + "\u0000" + name;
    }

    private void authorize(User user, File file, Permission permission) {
        if (!accessControl.hasPermission(user, file, permission)) {
            throw new AccessDeniedException(
                    user.getUsername() + " lacks " + permission + " permission on file " + file.getName());
        }
    }
}
