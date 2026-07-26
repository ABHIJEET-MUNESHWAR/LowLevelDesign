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

    /**
     * Creates a new active reservation with a fresh id, timestamped now, expiring after
     * {@code ttlSeconds}.
     *
     * @param storeId    the store the reserved stock belongs to
     * @param productId  the product being reserved
     * @param quantity   the quantity held by this reservation
     * @param ttlSeconds how many seconds from now this reservation should auto-expire if not
     *                   confirmed or cancelled first
     */
    public Reservation(String storeId, String productId, int quantity, long ttlSeconds) {
        this.reservationId = UUID.randomUUID().toString();
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(ttlSeconds);
        this.status = ReservationStatus.ACTIVE;
    }

    /**
     * Returns the unique identifier of this reservation.
     *
     * @return the reservation id
     */
    public String getReservationId() {
        return reservationId;
    }

    /**
     * Returns the id of the store the reserved stock belongs to.
     *
     * @return the store id
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Returns the id of the product being reserved.
     *
     * @return the product id
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the quantity held by this reservation.
     *
     * @return the reserved quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the instant at which this reservation auto-expires if not confirmed or cancelled.
     *
     * @return the expiry instant
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns the current lifecycle status of this reservation.
     *
     * @return the reservation status
     */
    public ReservationStatus getStatus() {
        return status;
    }

    /**
     * Updates the lifecycle status of this reservation (e.g. to CONFIRMED, CANCELLED, or EXPIRED).
     *
     * @param status the new status
     */
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    /**
     * Checks whether this reservation is still ACTIVE but has passed its expiry instant, meaning
     * it should be automatically released back to the available pool.
     *
     * @return true if the reservation is ACTIVE and {@code Instant.now()} is after {@code expiresAt}
     */
    public boolean isExpired() {
        return status == ReservationStatus.ACTIVE && Instant.now().isAfter(expiresAt);
    }
}
