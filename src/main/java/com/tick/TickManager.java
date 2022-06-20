package com.tick;

import com.model.Tick;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TickManager {

    private final Map<String, Tick> tickMap = new ConcurrentHashMap<>();
    private final List<TickObserver> tickObserverList = new ArrayList<>();
    public void offer(Tick tick){
        tickMap.put(tick.getExchange(), tick);

        Collection<Tick> latestValues = tickMap.values().stream()
                .sorted(Comparator.comparing(Tick::getExchange))
                .collect(Collectors.toList());

        for(TickObserver tickObserver : tickObserverList){
            tickObserver.onTickEvent(tick, latestValues);
        }
    }

    public void addTickObserver(TickObserver tickObserver){
        tickObserverList.add(tickObserver);
    }

}
