package com.lowleveldesign.elevatorlocus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single stop the elevator makes at a floor, capturing the minute it happened
 * and the passengers who alighted and/or boarded there.
 *
 * <p>Drop-offs ({@link Action#OUT}) are always reported before pickups
 * ({@link Action#IN}) at the same floor, matching the expected output ordering.
 */
public final class StopEvent {

    private final int time;
    private final int floor;
    private final List<String> alighting;
    private final List<String> boarding;

    public StopEvent(int time, int floor, List<String> alighting, List<String> boarding) {
        this.time = time;
        this.floor = floor;
        this.alighting = new ArrayList<>(alighting);
        this.boarding = new ArrayList<>(boarding);
    }

    public int getTime() {
        return time;
    }

    public int getFloor() {
        return floor;
    }

    public List<String> getAlighting() {
        return Collections.unmodifiableList(alighting);
    }

    public List<String> getBoarding() {
        return Collections.unmodifiableList(boarding);
    }

    /** @return {@code true} if nobody boarded or alighted at this stop. */
    public boolean isEmpty() {
        return alighting.isEmpty() && boarding.isEmpty();
    }

    /**
     * Renders this stop as the required output lines, e.g. {@code "5 A IN"}.
     * A stop where passengers both alight and board produces two lines
     * (OUT first, then IN).
     *
     * @return one line per action group performed at this floor
     */
    public List<String> toOutputLines() {
        List<String> lines = new ArrayList<>(2);
        if (!alighting.isEmpty()) {
            lines.add(floor + " " + String.join(" ", alighting) + " " + Action.OUT);
        }
        if (!boarding.isEmpty()) {
            lines.add(floor + " " + String.join(" ", boarding) + " " + Action.IN);
        }
        return lines;
    }

    @Override
    public String toString() {
        return "t=" + time + " " + String.join(" | ", toOutputLines());
    }
}
