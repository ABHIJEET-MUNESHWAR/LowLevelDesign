package com.lowleveldesign.userfilemanagement.exception;

/**
 * Thrown when an operation references a file that does not exist in the system.
 */
public class FileNotFoundException extends UserFileManagementException {

    private static final long serialVersionUID = 1L;

    /**
     * @param fileId the id of the file that could not be found
     */
    public FileNotFoundException(String fileId) {
        super("No file found with id: " + fileId);
    }
}
