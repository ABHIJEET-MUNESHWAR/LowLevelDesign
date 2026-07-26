package com.lowleveldesign.userfilemanagement.model;

/**
 * The role of a {@link User} within the system, which determines the
 * coarse-grained set of privileges the user is entitled to.
 *
 * <ul>
 *     <li>{@link #USER} - an ordinary account that owns its own files and may
 *     be granted access to files owned by others.</li>
 *     <li>{@link #ADMIN} - a privileged account that can manage user accounts
 *     and implicitly has full access to every file in the system.</li>
 * </ul>
 */
public enum Role {

    /** An ordinary user with rights limited to owned and explicitly shared files. */
    USER,

    /** An administrator who can manage accounts and access all files. */
    ADMIN
}
