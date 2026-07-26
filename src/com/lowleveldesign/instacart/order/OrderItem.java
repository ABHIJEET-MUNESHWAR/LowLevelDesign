package com.lowleveldesign.instacart.order;

/** A single line in an order/cart: one product and the desired quantity. */
public class OrderItem {
    private final String productId;
    private final int quantity;

    public OrderItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
