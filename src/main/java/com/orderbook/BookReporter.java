package com.orderbook;

import com.model.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BookReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.####");

    private final Book book;

    public BookReporter(Book book){
        this.book = book;
    }

    public void scheduleReport(int initialDelay){
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {

            List<Tick> bids = new ArrayList<>(book.getBids());
            List<Tick> asks = new ArrayList<>(book.getAsks());
            int length = Math.max(bids.size(), asks.size());

            LOGGER.info("====== " + book.getProduct() + " ======");

            for(int counter = 0; counter < length; counter++){
                LOGGER.info(
                        getString(bids, counter, Tick::getBid)
                         +" ::: "+
                        getString(asks, counter, Tick::getAsk));
            }
        }, initialDelay, 60, TimeUnit.SECONDS);
    }

    private String getString(List<Tick> ticks, int index, Function<Tick, Double> priceFunction){

        if(ticks.size() <= index){
            return "";
        }

        return ticks.get(index).getExchange() +"-"+ DECIMAL_FORMAT.format(priceFunction.apply(ticks.get(index)));
    }
}
