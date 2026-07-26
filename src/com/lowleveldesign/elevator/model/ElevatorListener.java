package com.lowleveldesign.elevator.model;

/**
 * Observer callback notified whenever an elevator opens its doors at a
 * floor. Used to model the real-world temporal constraint that a passenger's
 * destination request can only happen *after* they have actually boarded at
 * their pickup floor - never before.
 */
public interface ElevatorListener {

    /**
     * Invoked immediately after an elevator opens its doors at a floor — the
     * earliest point at which a passenger can be considered boarded, and
     * therefore the earliest a destination request may legitimately be raised.
     *
     * @param elevatorId the elevator that stopped
     * @param floor      the floor it stopped at
     */
    void onDoorOpened(int elevatorId, int floor);
}
