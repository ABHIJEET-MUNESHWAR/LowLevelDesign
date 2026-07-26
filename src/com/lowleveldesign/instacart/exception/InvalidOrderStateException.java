package com.lowleveldesign.instacart.exception;

import com.lowleveldesign.instacart.order.OrderStatus;

/** Thrown when an operation (e.g. checkout, cancel) is attempted on an order in an incompatible status. */
public class InvalidOrderStateException extends InstacartException {
    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, String attemptedAction) {
        super("Cannot " + attemptedAction + " order " + orderId + ": current status is " + currentStatus);
    }
}
