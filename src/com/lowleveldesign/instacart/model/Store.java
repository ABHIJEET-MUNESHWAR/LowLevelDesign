package com.lowleveldesign.instacart.model;

import com.lowleveldesign.instacart.exception.InvalidStoreException;

import java.util.Objects;

/**
 * A physical fulfillment location (grocery store / dark store) that Instacart
 * shops from. Each store maintains its own independent stock levels.
 */
public class Store {
    private final String storeId;
    private final String name;
    private final String address;

    /**
     * Creates a new store, validating its required fields up front so an invalid store can never
     * enter the system.
     *
     * @param storeId unique identifier for the store; must not be null or blank
     * @param name    display name; must not be null or blank
     * @param address physical address; may be null
     * @throws InvalidStoreException if the id or name is null or blank
     */
    public Store(String storeId, String name, String address) {
        if (storeId == null || storeId.trim().isEmpty()) {
            throw new InvalidStoreException("Store id must not be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidStoreException("Store name must not be null or blank");
        }
        this.storeId = storeId;
        this.name = name;
        this.address = address;
    }

    /**
     * Returns the unique identifier of this store.
     *
     * @return the store id
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Returns the display name of this store.
     *
     * @return the store name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the physical address of this store.
     *
     * @return the store address, or null if none was set
     */
    public String getAddress() {
        return address;
    }

    /**
     * Compares stores by {@code storeId} only, since a store is uniquely identified by its id
     * regardless of any other field.
     *
     * @param o the object to compare against
     * @return true if {@code o} is a Store with the same storeId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Store)) return false;
        Store store = (Store) o;
        return storeId.equals(store.storeId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)} (based on {@code storeId} only).
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(storeId);
    }

    /**
     * Returns a human-readable representation combining the store's name and id.
     *
     * @return a string of the form {@code "name (storeId)"}
     */
    @Override
    public String toString() {
        return name + " (" + storeId + ")";
    }
}
