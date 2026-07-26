package com.lowleveldesign.instacart.order;

/** A single line in an order/cart: one product and the desired quantity. */
public class OrderItem {
    private final String productId;
    private final int quantity;

    /**
     * Creates a new order line requesting a given quantity of a product.
     *
     * @param productId the product being requested
     * @param quantity  the quantity requested
     */
    public OrderItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * Returns the id of the requested product.
     *
     * @return the product id
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the requested quantity for this line item.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }
}
