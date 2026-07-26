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

    /**
     * Creates a new catalog product, validating its required fields up front so an invalid
     * product can never enter the system.
     *
     * @param productId unique catalog identifier; must not be null or blank
     * @param name      display name; must not be null or blank
     * @param category  merchandising category (e.g. "Produce"); may be null
     * @param price     unit price; must not be negative
     * @throws InvalidProductException if the id/name is null or blank, or the price is negative
     */
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

    /**
     * Returns the unique catalog identifier of this product.
     *
     * @return the product id
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the display name of this product.
     *
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the merchandising category of this product.
     *
     * @return the product category, or null if none was set
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the unit price of this product.
     *
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * Compares products by {@code productId} only, since a product is uniquely identified by its
     * catalog id regardless of any other field.
     *
     * @param o the object to compare against
     * @return true if {@code o} is a Product with the same productId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return productId.equals(product.productId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)} (based on {@code productId} only).
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    /**
     * Returns a human-readable representation combining the product's name and id.
     *
     * @return a string of the form {@code "name (productId)"}
     */
    @Override
    public String toString() {
        return name + " (" + productId + ")";
    }
}
