package com.model;

import java.time.Instant;
import java.util.Objects;

import static com.model.Product.Spot;

public class Tick {

    private final String exchange;
    private final Product product;
    private final Instant timestamp;
    private final double bid;
    private final double bidSize;
    private final double ask;
    private final double askSize;

    private Tick(Builder builder) {
        exchange = builder.exchange;
        product = builder.product;
        timestamp = builder.timestamp;
        bid = builder.bid;
        bidSize = builder.bidSize;
        ask = builder.ask;
        askSize = builder.askSize;
    }

    public String getExchange() {
        return exchange;
    }

    public Product getProduct() {
        return product;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getBidSize() {
        return bidSize;
    }

    public double getAskSize() {
        return askSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tick tick = (Tick) o;
        return Double.compare(tick.bidSize, bidSize) == 0 && Double.compare(tick.askSize, askSize) == 0 &&Double.compare(tick.bid, bid) == 0 && Double.compare(tick.ask, ask) == 0 && Objects.equals(exchange, tick.exchange) && product == tick.product && Objects.equals(timestamp, tick.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, product, timestamp, bid, ask, bidSize, askSize);
    }

    @Override
    public String toString() {
        return "{" +
                "exchange='" + exchange + '\'' +
                ", product=" + product +
                ", time=" + timestamp +
                ", bid=" + bid +
                ", bidSize=" + bidSize +
                ", ask=" + ask +
                ", askSize=" + askSize +
                '}';
    }

    public static final class Builder {
        private String exchange;
        private Product product = Spot;
        private Instant timestamp;
        private double bid;
        private double ask;
        private double bidSize;
        private double askSize;

        public Builder() {
        }

        public Builder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }

        public Builder product(Product product) {
            this.product = product;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder bid(double bid) {
            this.bid = bid;
            return this;
        }

        public Builder ask(double ask) {
            this.ask = ask;
            return this;
        }

        public Builder bidSize(double bidSize) {
            this.bidSize = bidSize;
            return this;
        }

        public Builder askSize(double askSize) {
            this.askSize = askSize;
            return this;
        }

        public Tick build() {
            return new Tick(this);
        }
    }
}
