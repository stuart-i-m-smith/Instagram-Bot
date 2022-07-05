package com.orderbook;

import com.model.Tick;

import java.util.Comparator;
import java.util.function.Function;

public class BookTickComparator implements Comparator<Tick> {

    private final Function<Tick, Double> priceFunction;
    private final Function<Tick, Double> sizeFunction;

    public BookTickComparator(Function<Tick, Double> priceFunction,
                              Function<Tick, Double> sizeFunction){
        this.priceFunction = priceFunction;
        this.sizeFunction = sizeFunction;
    }

    @Override
    public int compare(Tick t1, Tick t2) {
        int priceComparison = priceFunction.apply(t1).compareTo(priceFunction.apply(t2));
        
        if(priceComparison == 0){
            int sizeComparison = sizeFunction.apply(t1).compareTo(sizeFunction.apply(t2));

            if(sizeComparison == 0){
                return t1.getTimestamp().compareTo(t2.getTimestamp());
            }
        }

        return priceComparison;
    }
}
