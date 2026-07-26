package com.lowleveldesign.elevator.model;

/**
 * Direction of travel for an elevator or a hall (external) request.
 */
public enum Direction {
    /** Travelling toward higher floor numbers, or a passenger wanting to go up. */
    UP,
    /** Travelling toward lower floor numbers, or a passenger wanting to go down. */
    DOWN,
    /** Not travelling; also used for destination calls, which imply no direction. */
    IDLE
}
