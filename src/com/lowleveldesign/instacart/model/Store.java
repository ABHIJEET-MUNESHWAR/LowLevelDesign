package com.lowleveldesign.instacart.model;

import java.util.Objects;

/**
 * A physical fulfillment location (grocery store / dark store) that Instacart
 * shops from. Each store maintains its own independent stock levels.
 */
public class Store {
    private final String storeId;
    private final String name;
    private final String address;

    public Store(String storeId, String name, String address) {
        this.storeId = storeId;
        this.name = name;
        this.address = address;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Store)) return false;
        Store store = (Store) o;
        return storeId.equals(store.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId);
    }

    @Override
    public String toString() {
        return name + " (" + storeId + ")";
    }
}
