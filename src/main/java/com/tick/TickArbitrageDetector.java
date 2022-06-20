package com.tick;

import com.model.Tick;

import java.util.Collection;

public class TickArbitrageDetector implements TickObserver {

    private Tick lastHighestBid;
    private Tick lastLowestAsk;

    @Override
    public void onTickEvent(Tick tickEvent, Collection<Tick> allTicks) {
        if(allTicks.isEmpty()){
            return;
        }

        Tick highestBid = null;
        Tick lowestAsk = null;

        for(Tick tick : allTicks){
            if(highestBid == null || tick.getBid() > highestBid.getBid()){
                highestBid = tick;
            }

            if(lowestAsk == null || tick.getAsk() < lowestAsk.getAsk()){
                lowestAsk = tick;
            }
        }

        double arbValue = highestBid.getBid() - lowestAsk.getAsk();

        if(arbValue > 0){

            if(!highestBid.equals(lastHighestBid) || !lowestAsk.equals(lastLowestAsk)) {
                System.out.println("======");
                System.out.println("Arb detected: " + arbValue);
                System.out.println("Buy: " + lowestAsk);
                System.out.println("Sell: " + highestBid);
                System.out.println("======");

                lastHighestBid = highestBid;
                lastLowestAsk = lowestAsk;
            }
        }

    }
}
