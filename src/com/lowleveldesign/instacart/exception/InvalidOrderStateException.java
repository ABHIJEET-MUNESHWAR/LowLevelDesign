package com.lowleveldesign.instacart.exception;

import com.lowleveldesign.instacart.order.OrderStatus;

/** Thrown when an operation (e.g. checkout, cancel) is attempted on an order in an incompatible status. */
public class InvalidOrderStateException extends InstacartException {
    /**
     * Creates the exception for an operation attempted on an order in an incompatible status.
     *
     * @param orderId         the order id the operation was attempted on
     * @param currentStatus   the order's actual current status
     * @param attemptedAction a short description of the attempted action (e.g. "checkout", "cancel")
     */
    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, String attemptedAction) {
        super("Cannot " + attemptedAction + " order " + orderId + ": current status is " + currentStatus);
    }
}
