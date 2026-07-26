package com.lowleveldesign.instacart.exception;

/** Thrown when a reservation id passed to confirm/cancel does not correspond to any known reservation. */
public class ReservationNotFoundException extends InstacartException {
    public ReservationNotFoundException(String reservationId) {
        super("Unknown reservation: " + reservationId);
    }
}
