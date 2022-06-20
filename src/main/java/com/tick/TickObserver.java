package com.tick;

import com.model.Tick;

import java.util.Collection;

public interface TickObserver {
    void onTickEvent(Tick tickEvent, Collection<Tick> allTicks);
}
