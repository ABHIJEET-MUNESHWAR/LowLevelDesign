package com.lowleveldesign.instacart.model;

import java.util.Objects;

/**
 * Represents an item in the catalog (e.g. "Organic Bananas").
 * Immutable value object shared across all stores.
 */
public class Product {
    private final String productId;
    private final String name;
    private final String category;
    private final double price;

    public Product(String productId, String name, String category, double price) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return name + " (" + productId + ")";
    }
}
