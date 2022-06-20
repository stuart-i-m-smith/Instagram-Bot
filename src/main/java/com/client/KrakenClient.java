package com.client;

import com.tick.TickManager;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KrakenClient implements Client {

    private final TickManager tickManager;

    public KrakenClient(TickManager manager){
        this.tickManager = manager;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.kraken.com")) {

                private volatile double lastBid = 0;
                private volatile double lastAsk = 0;

                @Override
                public void onMessage(String message) {
                    if(!message.contains("\"event\":")){

                        JSONObject json = (JSONObject) new JSONObject("{\"pair\": "+message+"}")
                                .getJSONArray("pair")
                                .get(1);

                        JSONArray bids = json.getJSONArray("b");
                        JSONArray offers = json.getJSONArray("a");
                        double bid = bids.getDouble(0);
                        double ask = offers.getDouble(0);

                        if(!Precision.equals(lastBid, bid) ||
                            !Precision.equals(lastAsk, ask)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("kraken")
                                    .timestamp(Instant.now())
                                    .bid(bid)
                                    .ask(ask)
                                    .build();

                            lastBid = bid;
                            lastAsk = ask;

                            tickManager.offer(tick);
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to Kraken.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray pairs = new JSONArray();
            pairs.put("ADA/USD");

            JSONObject subscription = new JSONObject();
            subscription.put("name", "ticker");

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("event", "subscribe");
            subscribeMessage.put("pair", pairs);
            subscribeMessage.put("subscription", subscription);

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{\"event\": \"ping\"}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}