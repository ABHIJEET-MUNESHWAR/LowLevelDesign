package com.lowleveldesign.elevator.model;

/**
 * EXTERNAL  - a hall call raised from a floor (up/down button outside the elevator).
 * INTERNAL  - a destination call raised from inside the elevator cabin.
 */
public enum RequestType {
    /** A hall call raised from a floor's UP/DOWN button, outside the cabin. */
    EXTERNAL,
    /** A destination call raised from a floor button inside the cabin. */
    INTERNAL
}
