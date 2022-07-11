package com.model;

import java.util.Set;

public class TickEvent {

    private Set<Tick> ticks;

    public Set<Tick> getTicks() {
        return ticks;
    }

    public void setTicks(Set<Tick> ticks) {
        this.ticks = ticks;
    }

    @Override
    public String toString() {
        return "TickEvent{" +
                "ticks=" + ticks +
                '}';
    }
}
