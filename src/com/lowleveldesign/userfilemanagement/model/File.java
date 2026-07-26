package com.lowleveldesign.userfilemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a file stored in the system. A file has an immutable identity and
 * owner, mutable content and name, and audit timestamps that track when it was
 * created and last modified.
 */
public class File {

    private final String id;
    private String name;
    private String content;
    private final String ownerId;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates a file.
     *
     * @param id      unique identifier of the file
     * @param name    display name of the file
     * @param content initial textual content of the file
     * @param ownerId id of the {@link User} that owns the file
     */
    public File(String id, String name, String content, String ownerId) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** @return the file's unique identifier. */
    public String getId() {
        return id;
    }

    /** @return the file's display name. */
    public String getName() {
        return name;
    }

    /**
     * Renames the file and refreshes its last-modified timestamp.
     *
     * @param name the new file name
     */
    public void setName(String name) {
        this.name = name;
        touch();
    }

    /** @return the file's textual content. */
    public String getContent() {
        return content;
    }

    /**
     * Replaces the file's content and refreshes its last-modified timestamp.
     *
     * @param content the new content
     */
    public void setContent(String content) {
        this.content = content;
        touch();
    }

    /** @return the id of the user that owns the file. */
    public String getOwnerId() {
        return ownerId;
    }

    /** @return the instant the file was created. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return the instant the file was last modified. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Two files are equal if and only if they share the same {@code id}.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code File} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof File)) return false;
        File file = (File) o;
        return id.equals(file.id);
    }

    /** @return a hash code derived solely from the file's {@code id}. */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** @return a short {@code name(id)} representation of the file. */
    @Override
    public String toString() {
        return name + "(" + id + ")";
    }
}
