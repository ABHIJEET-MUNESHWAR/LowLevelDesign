package com.lowleveldesign.userfilemanagement.model;

/**
 * A fine-grained capability that can be granted to a {@link User} on a specific
 * {@link File}. The file owner (and any administrator) implicitly holds every
 * permission; other users hold only the permissions explicitly granted to them.
 */
public enum Permission {

    /** Allows reading the file's content and metadata. */
    READ,

    /** Allows modifying the file's content or name. */
    WRITE,

    /** Allows deleting the file. */
    DELETE,

    /** Allows granting or revoking permissions on the file to other users. */
    SHARE
}
