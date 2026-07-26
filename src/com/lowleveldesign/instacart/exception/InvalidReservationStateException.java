package com.lowleveldesign.instacart.exception;

import com.lowleveldesign.instacart.inventory.ReservationStatus;

/** Thrown when confirm/cancel is attempted on a reservation that is no longer ACTIVE. */
public class InvalidReservationStateException extends InstacartException {
    public InvalidReservationStateException(String reservationId, ReservationStatus currentStatus) {
        super("Reservation " + reservationId + " is not active (status=" + currentStatus + ")");
    }
}
