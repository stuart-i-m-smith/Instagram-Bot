package com;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.model.Tick;
import com.model.TickEvent;
import com.model.TickEventHandler;

public class TickEventProcessor {

    private final TickEventHandler[] tickEventHandlers;
    private RingBuffer<TickEvent> ringBuffer;

    public TickEventProcessor(TickEventHandler... tickEventHandlers){
        this.tickEventHandlers  = tickEventHandlers;
    }

    public void start(){

        int bufferSize = 1024;

        Disruptor<TickEvent> disruptor = new Disruptor<>(TickEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(tickEventHandlers);
        this.ringBuffer = disruptor.getRingBuffer();
        disruptor.start();
    }

    public void publishTick(Tick tick){
        ringBuffer.publishEvent((event, sequence, buffer) -> event.setTick(tick));
    }
}
