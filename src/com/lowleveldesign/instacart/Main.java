package com.lowleveldesign.instacart;

import com.lowleveldesign.instacart.inventory.InventoryManager;
import com.lowleveldesign.instacart.model.Product;
import com.lowleveldesign.instacart.model.Store;
import com.lowleveldesign.instacart.observer.LowStockNotifier;
import com.lowleveldesign.instacart.order.Order;
import com.lowleveldesign.instacart.order.OrderItem;
import com.lowleveldesign.instacart.order.OrderService;

import java.util.Arrays;

/** End-to-end demo of the inventory management system. */
public class Main {
    public static void main(String[] args) {
        InventoryManager inventoryManager = InventoryManager.getInstance();
        inventoryManager.registerObserver(new LowStockNotifier());

        Store store = new Store("store-1", "Whole Foods - Market St", "123 Market St");
        Product bananas = new Product("prod-1", "Organic Bananas", "Produce", 0.59);
        Product milk = new Product("prod-2", "Whole Milk 1gal", "Dairy", 3.99);

        inventoryManager.stockProduct(store, bananas, 20, 5);
        inventoryManager.stockProduct(store, milk, 10, 3);

        System.out.println("Initial state:");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        OrderService orderService = new OrderService(inventoryManager);

        // Customer places an order for bananas + milk.
        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(bananas.getProductId(), 8), new OrderItem(milk.getProductId(), 8)));
        System.out.println("\nAfter placing order (stock reserved, not yet deducted):");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        // Checkout confirms the order -> stock permanently deducted, may trigger low-stock alert.
        orderService.checkout(order);
        System.out.println("\nAfter checkout (order status=" + order.getStatus() + "):");
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), bananas.getProductId()));
        System.out.println(inventoryManager.getInventoryItem(store.getStoreId(), milk.getProductId()));

        // A second, oversized order fails atomically and rolls back any partial reservations.
        try {
            orderService.placeOrder("cust-2", store.getStoreId(),
                    Arrays.asList(new OrderItem(milk.getProductId(), 100)));
        } catch (RuntimeException e) {
            System.out.println("\nExpected failure: " + e.getMessage());
        }

        inventoryManager.shutdown();
    }
}
