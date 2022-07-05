package com.orderbook;

import com.model.Tick;

import java.util.Comparator;
import java.util.function.Function;

public class BookTickComparator implements Comparator<Tick> {

    private final Function<Tick, Double> priceFunction;

    public BookTickComparator(Function<Tick, Double> priceFunction){
        this.priceFunction = priceFunction;
    }

    @Override
    public int compare(Tick t1, Tick t2) {
        int priceComparison = priceFunction.apply(t1).compareTo(priceFunction.apply(t2));

        if(priceComparison == 0){
            return t1.getTimestamp().compareTo(t2.getTimestamp());
        }

        return priceComparison;
    }
}
