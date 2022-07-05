package com.client.websocket;

import com.TickEventProcessor;
import com.binance.connector.client.impl.WebsocketClientImpl;
import com.binance.connector.client.utils.WebSocketCallback;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;

import java.time.Instant;

public class BinanceClient implements Client {

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public BinanceClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {

        WebsocketClientImpl client = new WebsocketClientImpl();

        System.out.println("Connected to Binance.");

        client.bookTicker(currency + "usdt", new WebSocketCallback() {
            private double lastBid = 0;
            private double lastAsk = 0;

            @Override
            public void onReceive(String message) {
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

                    tickEventProcessor.publishTick(tick);
                }
            }
        });
    }
}