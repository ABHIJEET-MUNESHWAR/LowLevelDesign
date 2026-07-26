package com.lowleveldesign.instacart.exception;

/** Thrown when a reservation id passed to confirm/cancel does not correspond to any known reservation. */
public class ReservationNotFoundException extends InstacartException {
    /**
     * Creates the exception for an unknown reservation id.
     *
     * @param reservationId the reservation id that could not be found
     */
    public ReservationNotFoundException(String reservationId) {
        super("Unknown reservation: " + reservationId);
    }
}
