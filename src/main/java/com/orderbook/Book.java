package com.orderbook;

import com.model.Product;
import com.model.Tick;
import com.model.TickEvent;
import com.model.TickEventHandler;

import java.util.*;

public class Book implements TickEventHandler {

    private final Product product;

    private final Set<Tick> bids = new TreeSet<>(new BookTickComparator(Tick::getBid).reversed());
    private final Set<Tick> asks = new TreeSet<>(new BookTickComparator(Tick::getAsk));

    public Book(Product product){
        this.product = product;
    }

    @Override
    public void onEvent(TickEvent tickEvent, long l, boolean b) {
        if(product != tickEvent.getTick().getProduct()){
            return;
        }

        bids.removeIf(tick -> tick.getExchange().equals(tickEvent.getTick().getExchange()));
        asks.removeIf(tick -> tick.getExchange().equals(tickEvent.getTick().getExchange()));

        bids.add(tickEvent.getTick());
        asks.add(tickEvent.getTick());
    }

    public Collection<Tick> getBids(){
        return Collections.unmodifiableCollection(bids);
    }

    public Collection<Tick> getAsks(){
        return Collections.unmodifiableCollection(asks);
    }

    public Product getProduct() {
        return this.product;
    }
}
