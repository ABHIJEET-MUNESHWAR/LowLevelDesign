package com.lowleveldesign.instacart.model;

import com.lowleveldesign.instacart.exception.InvalidProductException;

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
        if (productId == null || productId.trim().isEmpty()) {
            throw new InvalidProductException("Product id must not be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidProductException("Product name must not be null or blank");
        }
        if (price < 0) {
            throw new InvalidProductException("Product price must not be negative: " + price);
        }
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
