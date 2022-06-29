package com.model;

import java.time.Instant;
import java.util.Objects;

public class Tick {

    private final String exchange;
    private final Instant timestamp;
    private final double bid;
    private final double ask;

    private Tick(Builder builder) {
        exchange = builder.exchange;
        timestamp = builder.timestamp;
        bid = builder.bid;
        ask = builder.ask;
    }

    public String getExchange() {
        return exchange;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tick tick = (Tick) o;
        return Double.compare(tick.bid, bid) == 0 && Double.compare(tick.ask, ask) == 0 && exchange.equals(tick.exchange) && timestamp.equals(tick.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, timestamp, bid, ask);
    }

    @Override
    public String toString() {
        return "{" +
                "exchange='" + exchange + '\'' +
                ", time=" + timestamp +
                ", bid=" + bid +
                ", ask=" + ask +
                '}';
    }

    public static final class Builder {
        private String exchange;
        private Instant timestamp;
        private double bid;
        private double ask;

        public Builder() {
        }

        public Builder exchange(String exchange) {
            this.exchange = exchange;
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

        public Tick build() {
            return new Tick(this);
        }
    }
}
