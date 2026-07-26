/**
 * Low Level Design of a User and File Management System.
 *
 * <p>The system lets ordinary users own files and perform CRUD operations on
 * them, lets owners share fine-grained permissions with other users, and lets
 * administrators manage accounts and access every file.
 *
 * <p>Supported capabilities:
 * <ul>
 *     <li>User registration, authentication (session-based) and self-service
 *     profile management (email, display name, password).</li>
 *     <li>File create, read, update, rename and delete, with names unique per
 *     owner.</li>
 *     <li>Permission-based access control (READ, WRITE, DELETE, SHARE) enforced
 *     on every file operation.</li>
 *     <li>Administrative account management: create users with roles, enable or
 *     disable accounts, change roles and list users.</li>
 * </ul>
 *
 * <p>Package layout:
 * <ul>
 *     <li>{@code model} - domain entities (User, File, Session) and enums
 *     (Role, Permission).</li>
 *     <li>{@code util} - the salted {@code PasswordHasher}.</li>
 *     <li>{@code exception} - domain-specific unchecked exceptions.</li>
 *     <li>{@code service} - UserService, AccessControlService, FileService and
 *     the FileManagementSystem facade that ties them together.</li>
 *     <li>{@code test} - a dependency-free TestRunner exercising the design.</li>
 * </ul>
 */
package com.lowleveldesign.userfilemanagement;
