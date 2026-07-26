# User and File Management System

A single-host, in-memory system for managing **users** and the **files** they own. Users register,
authenticate, and perform CRUD operations on their files; owners share fine-grained permissions with
other users; and administrators manage accounts and can access every file. Every file operation is
guarded by an explicit access-control check.

```
==== Sharing: Alice grants Bob READ ====
Bob can now read: 'first line\nsecond line'
Bob still cannot WRITE: bob lacks WRITE permission on file notes.txt

==== Admin overrides: admin can read and manage any file ====
Admin reads Alice's file: 'edited by bob'
```

## Table of Contents

1. [Requirements](#requirements)
2. [Package / Folder Details](#package--folder-details)
3. [Domain Model](#domain-model)
4. [Access Control Rules](#access-control-rules)
5. [Request Flow](#request-flow)
6. [Design Patterns](#design-patterns)
7. [How to Build and Run](#how-to-build-and-run)
8. [Testing](#testing)
9. [Extending the Design](#extending-the-design)

## Requirements

- **User management** — registration, session-based authentication, and self-service profile
  management (email, display name, password change).
- **File management** — create, read, update, rename and delete files. File names are unique per
  owner.
- **Access control** — a user may only interact with a file according to the permissions they hold
  (`READ`, `WRITE`, `DELETE`, `SHARE`). Owners and administrators hold all permissions implicitly.
- **Administration** — create accounts with a role, enable/disable accounts, change roles, and list
  users.
- **Testable, modular components** — each concern lives in its own class and is exercised by a
  dependency-free test suite.

## Package / Folder Details

| Package      | Type                        | Responsibility                                                                 |
|--------------|-----------------------------|--------------------------------------------------------------------------------|
| `model`      | `Role`, `Permission`        | Enums for coarse roles and fine-grained file capabilities.                     |
| `model`      | `User`, `File`, `Session`   | Core domain entities and the authenticated-session token.                      |
| `util`       | `PasswordHasher`            | Salted SHA-256 hashing and constant-time verification of passwords.            |
| `exception`  | `UserFileManagementException` + subtypes | Domain-specific unchecked exceptions.                             |
| `service`    | `UserService`               | Registration, authentication, sessions, profile and admin account operations. |
| `service`    | `AccessControlService`      | Grant table and the "may this user do X to this file?" decision.               |
| `service`    | `FileService`               | File CRUD and sharing, enforcing permissions on every call.                    |
| `service`    | `FileManagementSystem`      | Facade that wires the services together into one entry point.                  |
| `test`       | `TestRunner`                | Dependency-free correctness and concurrency tests.                             |
| root         | `Main`                      | End-to-end demonstration scenario.                                             |

## Domain Model

- **User** — id, unique username, salted password hash, email, display name, `Role`, and an `active`
  flag. Profile fields and the password are mutable; identity is not.
- **File** — id, name, content, immutable `ownerId`, plus `createdAt`/`updatedAt` audit timestamps.
- **Session** — an opaque token bound to a user id, issued on login and required by every operation.
- **Role** — `USER` or `ADMIN`.
- **Permission** — `READ`, `WRITE`, `DELETE`, `SHARE`.

## Access Control Rules

A single, uniform rule decides every file operation (`AccessControlService.hasPermission`):

1. An **administrator** implicitly holds every permission on every file.
2. A file's **owner** implicitly holds every permission on that file.
3. **Everyone else** holds only the permissions explicitly granted to them for that specific file.

| Operation            | Required permission |
|----------------------|---------------------|
| `readFile`           | `READ`              |
| `updateFile`         | `WRITE`             |
| `renameFile`         | `WRITE`             |
| `deleteFile`         | `DELETE`            |
| `grant`/`revoke`     | `SHARE`             |

Granting a permission never implies another (e.g. `WRITE` does not imply `SHARE`). Deleting a file
clears all of its grants so permissions cannot leak to a later, same-named file.

## Request Flow

A typical authorized operation, e.g. `FileManagementSystem.updateFile(session, fileId, content)`:

1. `FileService` asks `UserService.requireActiveUser(session)` to validate the token and resolve the
   acting, still-active user.
2. `FileService` loads the file (or throws `FileNotFoundException`).
3. `FileService` asks `AccessControlService.hasPermission(user, file, WRITE)`; a `false` result
   raises `AccessDeniedException`.
4. On success the domain object is mutated and its `updatedAt` timestamp refreshed.

## Design Patterns

- **Facade** — `FileManagementSystem` presents one cohesive API and hides the collaboration between
  the three services.
- **Service layer / Separation of concerns** — authentication, authorization and file storage are
  distinct, independently testable services.
- **Single Responsibility** — `AccessControlService` is the one place that answers authorization
  questions and owns the grant table.
- **Token / session** — stateless-looking operations authenticated by an opaque `Session` token.

## How to Build and Run

From the repository root (`src` as the source root):

```bash
# Compile just this package into out/ufm
javac -d out/ufm $(find src/com/lowleveldesign/userfilemanagement -name '*.java')

# Run the demonstration
java -cp out/ufm com.lowleveldesign.userfilemanagement.Main
```

On Windows PowerShell:

```powershell
$files = Get-ChildItem -Recurse src\com\lowleveldesign\userfilemanagement -Filter *.java |
    ForEach-Object { $_.FullName }
javac -d out\ufm $files
java -cp out\ufm com.lowleveldesign.userfilemanagement.Main
```

## Testing

`TestRunner` is a self-contained suite (no JUnit) covering registration, authentication, profile and
password changes, per-permission access control, sharing/revocation, admin overrides and guards,
account deactivation, grant cleanup on delete, listing scope, invalid/expired sessions, and a
concurrency race proving per-owner name uniqueness is atomic.

```bash
java -cp out/ufm com.lowleveldesign.userfilemanagement.test.TestRunner
# 16 passed, 0 failed
```

The runner exits non-zero if any test fails, so it is CI-friendly.

## Extending the Design

- **Persistence** — the in-memory maps in the services sit behind narrow interfaces; swap them for a
  repository backed by a database without touching callers.
- **Password policy** — replace the single SHA-256 pass in `PasswordHasher` with bcrypt/scrypt/Argon2.
- **Session expiry** — add a TTL and a `lastAccessedAt` to `Session` and evict on validation.
- **Group / role hierarchy** — extend `Permission` handling to support user groups or inherited roles.
- **Directories & versioning** — add a parent-folder reference to `File` and keep a content history.
