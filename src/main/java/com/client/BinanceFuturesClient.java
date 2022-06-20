package com.client;

import com.binance.connector.client.impl.WebsocketClientImpl;
import com.binance.connector.client.utils.WebSocketCallback;
import com.model.Tick;
import com.tick.TickManager;
import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;

import java.time.Instant;

public class BinanceFuturesClient implements Client {

    private final String currency;
    private final TickManager tickManager;

    public BinanceFuturesClient(String currency, TickManager manager){
        this.currency = currency;
        this.tickManager = manager;
    }

    @Override
    public void connect() {

        WebsocketClientImpl client = new WebsocketClientImpl("wss://fstream.binance.com");

        System.out.println("Connected to Binance Futures.");

        client.bookTicker(currency+"usdt", new WebSocketCallback() {
            private volatile double lastBid = 0;
            private volatile double lastAsk = 0;

            @Override
            public void onReceive(String message) {
                System.out.println(message);
                JSONObject json = new JSONObject(message);

                double bid = json.getDouble("b");
                double ask = json.getDouble("a");

                if(!Precision.equals(lastBid, bid) ||
                    !Precision.equals(lastAsk, ask)) {

                    Tick tick = new Tick.Builder()
                            .exchange("binance")
                            .timestamp(Instant.now())
                            .bid(bid)
                            .ask(ask)
                            .build();

                    lastBid = bid;
                    lastAsk = ask;

                    tickManager.offer(tick);
                }
            }
        });
    }
}