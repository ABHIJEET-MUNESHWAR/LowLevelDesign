package com.lowleveldesign.elevator.model;

/**
 * Observer callback notified whenever an elevator opens its doors at a
 * floor. Used to model the real-world temporal constraint that a passenger's
 * destination request can only happen *after* they have actually boarded at
 * their pickup floor - never before.
 */
public interface ElevatorListener {
    void onDoorOpened(int elevatorId, int floor);
}
