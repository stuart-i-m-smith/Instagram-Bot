package com.orderbook;

import com.model.Product;
import com.model.Tick;
import com.model.TickEvent;
import com.model.TickEventHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Book implements TickEventHandler {

    private static final Map<Product, Book> BOOK_MAP = new ConcurrentHashMap<>();
    private static final List<TickEventHandler> tickEventHandlers = new ArrayList<>();

    private final Product product;

    private final Set<Tick> bids = new TreeSet<>(new BookTickComparator(Tick::getBid, Tick::getBidSize).reversed());
    private final Set<Tick> asks = new TreeSet<>(new BookTickComparator(Tick::getAsk, Tick::getAskSize));

    public Book(Product product){
        this.product = product;
        BOOK_MAP.put(product, this);
    }

    @Override
    public void onEvent(TickEvent tickEvent, long l, boolean b) {

        for(Tick tick : tickEvent.getTicks()){

            if (product != tick.getProduct()) {
                return;
            }

            bids.removeIf(t -> t.getExchange().equals(tick.getExchange()));
            asks.removeIf(t -> t.getExchange().equals(tick.getExchange()));

            bids.add(tick);
            asks.add(tick);
        }

        tickEventHandlers.forEach(t -> t.onEvent(tickEvent, l, b));
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

    public static Book getBook(Product product){
        return BOOK_MAP.get(product);
    }

    public static void addListener(TickEventHandler tickEventHandler){
        tickEventHandlers.add(tickEventHandler);
    }
}
