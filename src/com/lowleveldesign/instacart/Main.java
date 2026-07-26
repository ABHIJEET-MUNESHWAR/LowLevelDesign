package com.lowleveldesign.instacart;

import com.lowleveldesign.instacart.exception.InstacartException;
import com.lowleveldesign.instacart.inventory.InventoryManager;
import com.lowleveldesign.instacart.model.Product;
import com.lowleveldesign.instacart.model.Store;
import com.lowleveldesign.instacart.observer.LowStockNotifier;
import com.lowleveldesign.instacart.order.Order;
import com.lowleveldesign.instacart.order.OrderItem;
import com.lowleveldesign.instacart.order.OrderService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** End-to-end demo of the inventory management system. */
public class Main {
    /**
     * Runs an end-to-end walkthrough of the inventory system: stocks two products, places and
     * checks out an order (showing reservation, low-stock alerting, order total, and
     * confirmation), demonstrates an admin adjusting a low-stock threshold, then shows atomic
     * rollback when an order requests more stock than is available.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        InventoryManager inventoryManager = InventoryManager.getInstance();
        inventoryManager.registerObserver(new LowStockNotifier());

        Store store = new Store("store-1", "Whole Foods - Market St", "123 Market St");
        Product bananas = new Product("prod-1", "Organic Bananas", "Produce", 0.59);
        Product milk = new Product("prod-2", "Whole Milk 1gal", "Dairy", 3.99);
        Map<String, Product> catalog = new HashMap<>();
        catalog.put(bananas.getProductId(), bananas);
        catalog.put(milk.getProductId(), milk);

        inventoryManager.stockProduct(store, bananas, 20, 5);
        inventoryManager.stockProduct(store, milk, 10, 3);

        System.out.println("Store: " + store.getName() + " (" + store.getAddress() + ")");
        System.out.println("Catalog: " + bananas.getName() + " [" + bananas.getCategory() + "] @ $"
                + bananas.getPrice() + ", " + milk.getName() + " [" + milk.getCategory() + "] @ $" + milk.getPrice());

        System.out.println("\nInitial state:");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        // Store admin tightens milk's low-stock threshold ahead of a promotion.
        inventoryManager.updateLowStockThreshold(store.getStoreId(), milk.getProductId(), 5);
        System.out.println("\nAdmin raised milk's low-stock threshold to 5.");

        OrderService orderService = new OrderService(inventoryManager);

        // Customer places an order for bananas + milk.
        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(bananas.getProductId(), 8), new OrderItem(milk.getProductId(), 8)));
        System.out.println("\nAfter placing order (stock reserved, not yet deducted):");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        double orderTotal = 0;
        for (OrderItem item : order.getItems()) {
            orderTotal += catalog.get(item.getProductId()).getPrice() * item.getQuantity();
        }
        System.out.printf("Order %s for customer %s totals $%.2f%n", order.getOrderId(), order.getCustomerId(),
                orderTotal);

        // Checkout confirms the order -> stock permanently deducted, may trigger low-stock alert.
        orderService.checkout(order);
        System.out.println("\nAfter checkout (order status=" + order.getStatus() + "):");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        // A second, oversized order fails atomically and rolls back any partial reservations.
        try {
            orderService.placeOrder("cust-2", store.getStoreId(),
                    Arrays.asList(new OrderItem(milk.getProductId(), 100)));
        } catch (InstacartException e) {
            System.out.println("\nExpected failure: " + e.getMessage());
        }

        inventoryManager.shutdown();
    }
}
