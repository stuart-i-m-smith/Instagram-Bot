package com.model;

import com.lmax.disruptor.EventHandler;

public interface TickEventHandler extends EventHandler<TickEvent> {
    @Override
    void onEvent(TickEvent tickEvent, long l, boolean b);
}
