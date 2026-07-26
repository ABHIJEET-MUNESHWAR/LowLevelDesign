package com.lowleveldesign.elevatorlocus.exception;

/**
 * Thrown when an elevator request is malformed or violates the problem's
 * constraints (bad floor bounds, direction/destination mismatch, etc.).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
