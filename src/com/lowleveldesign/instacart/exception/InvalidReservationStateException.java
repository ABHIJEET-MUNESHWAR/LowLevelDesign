package com.lowleveldesign.instacart.exception;

import com.lowleveldesign.instacart.inventory.ReservationStatus;

/** Thrown when confirm/cancel is attempted on a reservation that is no longer ACTIVE. */
public class InvalidReservationStateException extends InstacartException {
    /**
     * Creates the exception for a confirm/cancel attempt on a non-ACTIVE reservation.
     *
     * @param reservationId the reservation id the operation was attempted on
     * @param currentStatus the reservation's actual current status
     */
    public InvalidReservationStateException(String reservationId, ReservationStatus currentStatus) {
        super("Reservation " + reservationId + " is not active (status=" + currentStatus + ")");
    }
}
