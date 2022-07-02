package com;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.model.Tick;
import com.model.TickEvent;
import com.model.TickEventHandler;

public class TickEventProcessor {

    private Disruptor<TickEvent> disruptor;
    private RingBuffer<TickEvent> ringBuffer;

    public void start(TickEventHandler... tickEventHandlers){

        int bufferSize = 1024;

        this.disruptor = new Disruptor<>(TickEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);
        this.disruptor.handleEventsWith(tickEventHandlers);
        this.ringBuffer = disruptor.getRingBuffer();
        this.disruptor.start();
    }

    public void publishTick(Tick tick){
        ringBuffer.publishEvent((event, sequence, buffer) -> event.setTick(tick));
    }
}
