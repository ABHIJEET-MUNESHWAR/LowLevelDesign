package com.lowleveldesign.instacart.inventory;

import java.time.Instant;
import java.util.UUID;

/** Represents a temporary hold of stock (e.g. items sitting in a shopper's cart). */
public class Reservation {
    private final String reservationId;
    private final String storeId;
    private final String productId;
    private final int quantity;
    private final Instant createdAt;
    private final Instant expiresAt;
    private volatile ReservationStatus status;

    public Reservation(String storeId, String productId, int quantity, long ttlSeconds) {
        this.reservationId = UUID.randomUUID().toString();
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(ttlSeconds);
        this.status = ReservationStatus.ACTIVE;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public boolean isExpired() {
        return status == ReservationStatus.ACTIVE && Instant.now().isAfter(expiresAt);
    }
}
