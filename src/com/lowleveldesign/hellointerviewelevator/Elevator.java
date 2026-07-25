package com.lowleveldesign.elevator;


/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
    Date:   25-07-2026
    Time:   06:13 pm
*/

import java.util.*;

public class Elevator {
    private int          currentFloor;
    private Direction    direction;
    private Set<Request> requests;

    public Elevator() {
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.requests = new HashSet<>();
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }
}
