package com.lowleveldesign.elevator.model;

/**
 * EXTERNAL  - a hall call raised from a floor (up/down button outside the elevator).
 * INTERNAL  - a destination call raised from inside the elevator cabin.
 */
public enum RequestType {
    EXTERNAL,
    INTERNAL
}
