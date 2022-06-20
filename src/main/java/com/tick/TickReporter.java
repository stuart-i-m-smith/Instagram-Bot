package com.tick;

import com.model.Tick;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TickReporter implements TickObserver{

    private volatile Collection<Tick> latestTicks = Collections.emptyList();

    @Override
    public void onTickEvent(Tick tickEvent, Collection<Tick> allTicks) {
        latestTicks = allTicks;
    }

    public void scheduleReport(){
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                for(Tick tick : latestTicks){
                    System.out.println(tick);
                }

                System.out.println("======");
            }, 15, 60, TimeUnit.SECONDS);
    }
}
