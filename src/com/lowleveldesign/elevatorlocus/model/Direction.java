package com.lowleveldesign.elevatorlocus.model;

/**
 * Direction of travel for the elevator, or the desired direction of a hall
 * (external) request pressed by a passenger.
 */
public enum Direction {

    /** Moving toward higher floor numbers. */
    UP(1),

    /** Moving toward lower floor numbers. */
    DOWN(-1),

    /** Stationary; the elevator has no work to do. */
    IDLE(0);

    private final int step;

    Direction(int step) {
        this.step = step;
    }

    /**
     * @return the signed floor delta applied for one minute of travel in this
     *         direction ({@code +1} for {@link #UP}, {@code -1} for
     *         {@link #DOWN}, {@code 0} for {@link #IDLE}).
     */
    public int step() {
        return step;
    }

    /**
     * @return the opposite travel direction; {@link #IDLE} maps to itself.
     */
    public Direction reverse() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            default:
                return IDLE;
        }
    }
}
