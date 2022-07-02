package com.model;

public class TickEvent {

    private Tick tick;

    public Tick getTick() {
        return tick;
    }

    public void setTick(Tick tick) {
        this.tick = tick;
    }

    @Override
    public String toString() {
        return "TickEvent{" +
                "tick=" + tick +
                '}';
    }
}
