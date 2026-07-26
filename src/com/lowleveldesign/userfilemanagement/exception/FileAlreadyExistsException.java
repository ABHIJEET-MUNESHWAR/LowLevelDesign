package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when a user tries to create a file whose name collides with another
 * file they already own. File names are unique per owner.
 */
public class FileAlreadyExistsException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param name the file name that already exists for the owner
     */
    public FileAlreadyExistsException(String name) {
        super("A file with this name already exists for the owner: " + name);
    }
}
