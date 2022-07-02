package com.tick;

import com.model.Tick;
import com.model.TickEvent;
import com.model.TickEventHandler;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TickReporter implements TickEventHandler {

    private final Collection<Tick> latestTicks = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(TickEvent tickEvent, long l, boolean b) {
        latestTicks.add(tickEvent.getTick());
    }

    public void scheduleReport(String tickType, int initialDelay){
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                System.out.println("====== " + tickType + " ======");
                for(Tick tick : latestTicks){
                    System.out.println(tick);
                }
            }, initialDelay, 60, TimeUnit.SECONDS);
    }
}
