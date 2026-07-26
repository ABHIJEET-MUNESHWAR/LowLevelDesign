package com.lowleveldesign.instacart.test;

import com.lowleveldesign.instacart.exception.InsufficientStockException;
import com.lowleveldesign.instacart.exception.InvalidOrderException;
import com.lowleveldesign.instacart.exception.InvalidOrderStateException;
import com.lowleveldesign.instacart.exception.InvalidProductException;
import com.lowleveldesign.instacart.exception.InvalidReservationStateException;
import com.lowleveldesign.instacart.exception.InvalidStoreException;
import com.lowleveldesign.instacart.exception.ProductNotTrackedException;
import com.lowleveldesign.instacart.exception.ReservationNotFoundException;
import com.lowleveldesign.instacart.inventory.InventoryItem;
import com.lowleveldesign.instacart.inventory.InventoryManager;
import com.lowleveldesign.instacart.inventory.Reservation;
import com.lowleveldesign.instacart.model.Product;
import com.lowleveldesign.instacart.model.Store;
import com.lowleveldesign.instacart.observer.InventoryObserver;
import com.lowleveldesign.instacart.order.Order;
import com.lowleveldesign.instacart.order.OrderItem;
import com.lowleveldesign.instacart.order.OrderService;
import com.lowleveldesign.instacart.order.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free correctness and concurrency test suite for the instacart inventory package (no
 * JUnit required, targets Java 8). Run via
 * {@code java com.lowleveldesign.instacart.test.TestRunner}; exits non-zero on failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static int idCounter = 0;

    /**
     * Runs every test in the suite and prints a pass/fail line per test plus a final tally.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        run("stocking a product makes it fully available", TestRunner::testStockAndAvailableQuantity);
        run("reserving stock reduces available but not total", TestRunner::testReserveReducesAvailable);
        run("reserving more than available throws InsufficientStockException",
                TestRunner::testReserveInsufficientStockThrows);
        run("confirming a reservation permanently deducts stock", TestRunner::testConfirmDeductsStock);
        run("cancelling a reservation releases stock back to available", TestRunner::testCancelReleasesStock);
        run("confirming the same reservation twice throws", TestRunner::testDoubleConfirmThrows);
        run("cancelling an unknown reservation throws", TestRunner::testCancelUnknownReservationThrows);
        run("restock increases total and available quantity", TestRunner::testRestockIncreasesQuantity);
        run("low-stock observer is notified once threshold is breached", TestRunner::testLowStockObserverNotified);
        run("querying an untracked product throws", TestRunner::testUntrackedProductThrows);
        run("InventoryItem.isBelowThreshold reflects available quantity", TestRunner::testIsBelowThreshold);
        run("placeOrder reserves stock for every line item", TestRunner::testPlaceOrderReservesAllItems);
        run("placeOrder rolls back all reservations if one item is short", TestRunner::testPlaceOrderRollsBackOnFailure);
        run("checkout confirms every reservation and deducts stock", TestRunner::testCheckoutConfirmsOrder);
        run("cancelOrder releases every reservation", TestRunner::testCancelOrderReleasesAllItems);
        run("concurrent reservations for limited stock never oversell", TestRunner::testConcurrentReservationRace);
        run("concurrent reservations on different products all succeed", TestRunner::testConcurrentDifferentProducts);
        run("creating a Product with blank id throws InvalidProductException", TestRunner::testInvalidProductBlankId);
        run("creating a Product with negative price throws InvalidProductException",
                TestRunner::testInvalidProductNegativePrice);
        run("creating a Store with blank id throws InvalidStoreException", TestRunner::testInvalidStoreBlankId);
        run("placeOrder with no line items throws InvalidOrderException", TestRunner::testPlaceOrderEmptyItemsThrows);
        run("checkout on an already-confirmed order throws InvalidOrderStateException",
                TestRunner::testCheckoutTwiceThrows);
        run("cancelOrder on an already-cancelled order throws InvalidOrderStateException",
                TestRunner::testCancelOrderTwiceThrows);
        run("Product getters return the values passed to its constructor", TestRunner::testProductGetters);
        run("Store getters return the values passed to its constructor", TestRunner::testStoreGetters);
        run("Order exposes the customerId and items it was constructed with", TestRunner::testOrderGetters);
        run("a reservation's expiry instant is set from its TTL", TestRunner::testReservationExpiresAt);
        run("updateLowStockThreshold changes when low-stock alerts fire", TestRunner::testUpdateLowStockThreshold);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        // The manager's background scheduler thread is non-daemon; shut it down so the JVM exits.
        InventoryManager.getInstance().shutdown();
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Executes a single test, records the outcome, and prints its result line.
     *
     * <p>Catches {@link Throwable} rather than {@link Exception} so an {@link AssertionError} from
     * a failed assertion is reported as a normal test failure instead of aborting the whole run.
     *
     * @param name the human-readable test description to print
     * @param test the test body; returning normally means pass, throwing means fail
     */
    private static void run(String name, Callable<Void> test) {
        try {
            test.call();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " -- " + t.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /** Generates a fresh store id, product id, and product for a single test, avoiding cross-test collisions. */
    private static synchronized String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }

    private static Store newStore() {
        String id = nextId("store");
        return new Store(id, "Test Store " + id, "1 Test St");
    }

    private static Product newProduct() {
        String id = nextId("prod");
        return new Product(id, "Test Product " + id, "Test Category", 1.99);
    }

    // ------------------------------------------------------------------
    // InventoryManager / InventoryItem tests
    // ------------------------------------------------------------------

    private static Void testStockAndAvailableQuantity() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();

        manager.stockProduct(store, product, 20, 5);

        assertEquals(20, manager.getAvailableQuantity(store.getStoreId(), product.getProductId()),
                "Newly stocked product should be fully available");
        return null;
    }

    private static Void testReserveReducesAvailable() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        manager.reserveStock(store.getStoreId(), product.getProductId(), 8, 60);

        InventoryItem item = manager.getInventoryItem(store.getStoreId(), product.getProductId());
        assertEquals(12, item.getAvailableQuantity(), "Available quantity should drop by reserved amount");
        assertEquals(20, item.getTotalQuantity(), "Total quantity should be unaffected by a reservation");
        return null;
    }

    private static Void testReserveInsufficientStockThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 5, 1);

        try {
            manager.reserveStock(store.getStoreId(), product.getProductId(), 6, 60);
            throw new AssertionError("Expected InsufficientStockException when reserving more than available");
        } catch (InsufficientStockException expected) {
            // expected
        }
        // The failed attempt must not have partially reserved anything.
        assertEquals(5, manager.getAvailableQuantity(store.getStoreId(), product.getProductId()),
                "A failed reservation should leave available quantity untouched");
        return null;
    }

    private static Void testConfirmDeductsStock() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Reservation reservation = manager.reserveStock(store.getStoreId(), product.getProductId(), 8, 60);
        manager.confirmReservation(reservation.getReservationId());

        InventoryItem item = manager.getInventoryItem(store.getStoreId(), product.getProductId());
        assertEquals(12, item.getTotalQuantity(), "Confirming should permanently remove stock from the total");
        assertEquals(12, item.getAvailableQuantity(), "Available should equal total once reservation is confirmed");
        return null;
    }

    private static Void testCancelReleasesStock() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Reservation reservation = manager.reserveStock(store.getStoreId(), product.getProductId(), 8, 60);
        manager.cancelReservation(reservation.getReservationId());

        InventoryItem item = manager.getInventoryItem(store.getStoreId(), product.getProductId());
        assertEquals(20, item.getAvailableQuantity(), "Cancelling should return stock to the available pool");
        assertEquals(20, item.getTotalQuantity(), "Cancelling must never touch total quantity");
        return null;
    }

    private static Void testDoubleConfirmThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Reservation reservation = manager.reserveStock(store.getStoreId(), product.getProductId(), 4, 60);
        manager.confirmReservation(reservation.getReservationId());
        try {
            manager.confirmReservation(reservation.getReservationId());
            throw new AssertionError(
                    "Expected InvalidReservationStateException confirming an already-confirmed reservation");
        } catch (InvalidReservationStateException expected) {
            // expected
        }
        return null;
    }

    private static Void testCancelUnknownReservationThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        try {
            manager.cancelReservation("does-not-exist");
            throw new AssertionError("Expected ReservationNotFoundException for an unknown reservation id");
        } catch (ReservationNotFoundException expected) {
            // expected
        }
        return null;
    }

    private static Void testRestockIncreasesQuantity() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 10, 2);

        manager.restock(store.getStoreId(), product.getProductId(), 15);

        assertEquals(25, manager.getAvailableQuantity(store.getStoreId(), product.getProductId()),
                "Restocking should add to total (and thus available) quantity");
        return null;
    }

    private static Void testLowStockObserverNotified() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 10, 3);

        List<InventoryItem> notifications = new CopyOnWriteArrayList<>();
        InventoryObserver observer = notifications::add;
        manager.registerObserver(observer);

        // Reserving 8 of 10 leaves 2 available, at/below the threshold of 3 -> should notify.
        manager.reserveStock(store.getStoreId(), product.getProductId(), 8, 60);

        boolean notifiedForThisProduct = notifications.stream()
                .anyMatch(item -> item.getProduct().getProductId().equals(product.getProductId()));
        assertTrue(notifiedForThisProduct, "Observer should be notified once stock drops at/below threshold");
        return null;
    }

    private static Void testUntrackedProductThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        try {
            manager.getAvailableQuantity("no-such-store", "no-such-product");
            throw new AssertionError("Expected ProductNotTrackedException for an untracked (store, product) pair");
        } catch (ProductNotTrackedException expected) {
            // expected
        }
        return null;
    }

    private static Void testIsBelowThreshold() {
        Store store = newStore();
        Product product = newProduct();
        InventoryItem item = new InventoryItem(store, product, 10, 3);

        assertTrue(!item.isBelowThreshold(), "10 available with threshold 3 should not be low stock");
        assertTrue(item.reserve(8), "Reserving 8 of 10 should succeed");
        assertTrue(item.isBelowThreshold(), "2 available with threshold 3 should be flagged as low stock");
        item.release(8);
        assertTrue(!item.isBelowThreshold(), "Releasing the reservation should clear the low-stock condition");
        return null;
    }

    // ------------------------------------------------------------------
    // OrderService tests
    // ------------------------------------------------------------------

    private static Void testPlaceOrderReservesAllItems() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product bananas = newProduct();
        Product milk = newProduct();
        manager.stockProduct(store, bananas, 20, 5);
        manager.stockProduct(store, milk, 10, 3);

        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(bananas.getProductId(), 8), new OrderItem(milk.getProductId(), 4)));

        assertEquals(2, order.getReservationIds().size(), "Order should hold one reservation per line item");
        assertEquals(12, manager.getAvailableQuantity(store.getStoreId(), bananas.getProductId()),
                "Bananas available quantity should reflect the reservation");
        assertEquals(6, manager.getAvailableQuantity(store.getStoreId(), milk.getProductId()),
                "Milk available quantity should reflect the reservation");
        return null;
    }

    private static Void testPlaceOrderRollsBackOnFailure() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product bananas = newProduct();
        Product milk = newProduct();
        manager.stockProduct(store, bananas, 20, 5);
        manager.stockProduct(store, milk, 3, 1);

        try {
            orderService.placeOrder("cust-1", store.getStoreId(), Arrays.asList(
                    new OrderItem(bananas.getProductId(), 8),   // would succeed on its own
                    new OrderItem(milk.getProductId(), 100)));  // impossible -> triggers rollback
            throw new AssertionError("Expected InsufficientStockException for the oversized line item");
        } catch (InsufficientStockException expected) {
            // expected
        }

        // The bananas reservation made before the failure must have been rolled back.
        assertEquals(20, manager.getAvailableQuantity(store.getStoreId(), bananas.getProductId()),
                "Successful reservations made before a failing item must be rolled back");
        assertEquals(3, manager.getAvailableQuantity(store.getStoreId(), milk.getProductId()),
                "Milk stock should be untouched since its reservation never succeeded");
        return null;
    }

    private static Void testCheckoutConfirmsOrder() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(product.getProductId(), 8)));
        orderService.checkout(order);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus(), "Order status should be CONFIRMED after checkout");
        InventoryItem item = manager.getInventoryItem(store.getStoreId(), product.getProductId());
        assertEquals(12, item.getTotalQuantity(), "Checkout should permanently deduct stock");
        assertEquals(12, item.getAvailableQuantity(), "Available should equal total once checked out");
        return null;
    }

    private static Void testCancelOrderReleasesAllItems() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(product.getProductId(), 8)));
        orderService.cancelOrder(order);

        assertEquals(OrderStatus.CANCELLED, order.getStatus(), "Order status should be CANCELLED after cancelOrder");
        assertEquals(20, manager.getAvailableQuantity(store.getStoreId(), product.getProductId()),
                "Cancelling an order should return all reserved stock to the available pool");
        return null;
    }

    // ------------------------------------------------------------------
    // Concurrency tests
    // ------------------------------------------------------------------

    /**
     * Verifies the critical concurrency guarantee: when many threads race to reserve more stock
     * than exists in total, the number of successful reservations is bounded exactly by available
     * stock -- no overselling, and no lost updates.
     */
    private static Void testConcurrentReservationRace() throws Exception {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 10, 2); // only 10 units; each reservation takes 1

        int threadCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    manager.reserveStock(store.getStoreId(), product.getProductId(), 1, 60);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ignored) {
                    // expected for the losers once stock runs out
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(10, successCount.get(), "Exactly 10 of 30 racers should win a unit of the 10 in stock");
        assertEquals(0, manager.getAvailableQuantity(store.getStoreId(), product.getProductId()),
                "All stock should be reserved with none left over and none oversold");
        return null;
    }

    /**
     * Verifies that per-item locking does not serialize unrelated work: concurrent reservations
     * against different products all succeed independently.
     */
    private static Void testConcurrentDifferentProducts() throws Exception {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        int productCount = 8;
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < productCount; i++) {
            Product product = newProduct();
            manager.stockProduct(store, product, 10, 2);
            products.add(product);
        }

        ExecutorService pool = Executors.newFixedThreadPool(productCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Product product : products) {
            tasks.add(() -> {
                manager.reserveStock(store.getStoreId(), product.getProductId(), 4, 60);
                successCount.incrementAndGet();
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(productCount, successCount.get(), "Each thread reserves its own product, all should succeed");
        return null;
    }

    // ------------------------------------------------------------------
    // Custom exception / validation tests
    // ------------------------------------------------------------------

    private static Void testInvalidProductBlankId() {
        try {
            new Product("  ", "Bad Product", "Category", 1.0);
            throw new AssertionError("Expected InvalidProductException for a blank product id");
        } catch (InvalidProductException expected) {
            // expected
        }
        return null;
    }

    private static Void testInvalidProductNegativePrice() {
        try {
            new Product(nextId("prod"), "Bad Product", "Category", -0.01);
            throw new AssertionError("Expected InvalidProductException for a negative price");
        } catch (InvalidProductException expected) {
            // expected
        }
        return null;
    }

    private static Void testInvalidStoreBlankId() {
        try {
            new Store("", "Bad Store", "Nowhere");
            throw new AssertionError("Expected InvalidStoreException for a blank store id");
        } catch (InvalidStoreException expected) {
            // expected
        }
        return null;
    }

    private static Void testPlaceOrderEmptyItemsThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        try {
            orderService.placeOrder("cust-1", store.getStoreId(), new ArrayList<>());
            throw new AssertionError("Expected InvalidOrderException for an order with no line items");
        } catch (InvalidOrderException expected) {
            // expected
        }
        try {
            orderService.placeOrder("cust-1", store.getStoreId(), null);
            throw new AssertionError("Expected InvalidOrderException for a null item list");
        } catch (InvalidOrderException expected) {
            // expected
        }
        return null;
    }

    private static Void testCheckoutTwiceThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(product.getProductId(), 4)));
        orderService.checkout(order);
        try {
            orderService.checkout(order);
            throw new AssertionError("Expected InvalidOrderStateException checking out an already-confirmed order");
        } catch (InvalidOrderStateException expected) {
            // expected
        }
        return null;
    }

    private static Void testCancelOrderTwiceThrows() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Order order = orderService.placeOrder("cust-1", store.getStoreId(),
                Arrays.asList(new OrderItem(product.getProductId(), 4)));
        orderService.cancelOrder(order);
        try {
            orderService.cancelOrder(order);
            throw new AssertionError("Expected InvalidOrderStateException cancelling an already-cancelled order");
        } catch (InvalidOrderStateException expected) {
            // expected
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Getter / accessor coverage tests
    // ------------------------------------------------------------------

    private static Void testProductGetters() {
        Product product = new Product(nextId("prod"), "Organic Bananas", "Produce", 0.59);
        assertEquals("Organic Bananas", product.getName(), "getName should return the constructor value");
        assertEquals("Produce", product.getCategory(), "getCategory should return the constructor value");
        assertEquals(0.59, product.getPrice(), "getPrice should return the constructor value");
        return null;
    }

    private static Void testStoreGetters() {
        Store store = new Store(nextId("store"), "Whole Foods", "123 Market St");
        assertEquals("Whole Foods", store.getName(), "getName should return the constructor value");
        assertEquals("123 Market St", store.getAddress(), "getAddress should return the constructor value");
        return null;
    }

    private static Void testOrderGetters() {
        InventoryManager manager = InventoryManager.getInstance();
        OrderService orderService = new OrderService(manager);
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        List<OrderItem> items = Arrays.asList(new OrderItem(product.getProductId(), 4));
        Order order = orderService.placeOrder("cust-42", store.getStoreId(), items);

        assertEquals("cust-42", order.getCustomerId(), "getCustomerId should return the constructor value");
        assertEquals(items, order.getItems(), "getItems should return the exact line items the order was placed with");
        assertEquals(store.getStoreId(), order.getStoreId(), "getStoreId should return the constructor value");
        return null;
    }

    private static Void testReservationExpiresAt() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 20, 5);

        Instant before = Instant.now();
        Reservation reservation = manager.reserveStock(store.getStoreId(), product.getProductId(), 4, 60);

        assertTrue(reservation.getExpiresAt().isAfter(before),
                "A reservation's expiry instant should be in the future relative to when it was created");
        assertTrue(reservation.getExpiresAt().isBefore(before.plusSeconds(61)),
                "A reservation's expiry instant should honor the requested TTL (60s)");
        return null;
    }

    private static Void testUpdateLowStockThreshold() {
        InventoryManager manager = InventoryManager.getInstance();
        Store store = newStore();
        Product product = newProduct();
        manager.stockProduct(store, product, 10, 2);

        InventoryItem item = manager.getInventoryItem(store.getStoreId(), product.getProductId());
        // 8 available (after reserving 2) is not below the original threshold of 2.
        manager.reserveStock(store.getStoreId(), product.getProductId(), 2, 60);
        assertTrue(!item.isBelowThreshold(), "8 available should not be low stock with the original threshold of 2");

        // Raising the threshold to 8 should immediately flag the same available quantity as low.
        manager.updateLowStockThreshold(store.getStoreId(), product.getProductId(), 8);
        assertEquals(8, item.getLowStockThreshold(), "getLowStockThreshold should reflect the update");
        assertTrue(item.isBelowThreshold(), "8 available should be low stock once the threshold is raised to 8");
        return null;
    }
}

