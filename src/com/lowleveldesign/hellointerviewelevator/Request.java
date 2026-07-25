package com.lowleveldesign.elevator;


/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
    Date:   25-07-2026
    Time:   06:10 pm
*/

import java.util.Objects;

public class Request {
    private int         floor;
    private RequestType type;

    public Request(int floor, RequestType type) {
        this.floor = floor;
        this.type = type;
    }

    public int getFloor() {
        return floor;
    }

    public RequestType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return floor == request.floor && type == request.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, type);
    }
}
