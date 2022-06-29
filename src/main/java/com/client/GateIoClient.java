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
import java.util.concurrent.TimeUnit;

public class GateIoClient implements Client {

    private final TickManager tickManager;
    private final String currency;

    public GateIoClient(String currency,
                        TickManager manager){
        this.currency = currency;
        this.tickManager = manager;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.gate.io/v3/")) {

                private volatile double lastBid = 0;
                private volatile double lastAsk = 0;

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);

                    if(json.has("method") && "depth.update".equals(json.getString("method"))){
                        JSONArray params = json.getJSONArray("params");
                        JSONObject topOfBook = params.getJSONObject(1);

                        if(topOfBook.has("bids") || topOfBook.has("asks")){
                            Tick.Builder tickBuilder = new Tick.Builder()
                                .exchange("gateio");

                            if(topOfBook.has("current")) {
                                tickBuilder.timestamp(Instant.ofEpochMilli(((Number) topOfBook.getDouble("current")).longValue() * 1000));
                            }else{
                                tickBuilder.timestamp(Instant.now());
                            }

                            boolean isUpdated = false;

                            if(topOfBook.has("bids")){
                                double bid = topOfBook.getJSONArray("bids")
                                        .getJSONArray(0)
                                        .getDouble(0);

                                if(!Precision.equals(lastBid, bid)) {
                                    lastBid = bid;
                                    tickBuilder.bid(bid);
                                    isUpdated = true;
                                }else{
                                    tickBuilder.bid(lastBid);
                                }
                            }else{
                                tickBuilder.bid(lastBid);
                            }

                            if(topOfBook.has("asks")){
                                double ask = topOfBook.getJSONArray("asks")
                                        .getJSONArray(0)
                                        .getDouble(0);

                                if(!Precision.equals(lastAsk, ask)) {
                                    lastAsk = ask;
                                    tickBuilder.ask(ask);
                                    isUpdated = true;
                                }else{
                                    tickBuilder.ask(lastAsk);
                                }
                            }else{
                                tickBuilder.ask(lastAsk);
                            }

                            Tick tick = tickBuilder.build();

                            if(isUpdated) {
                                tickManager.offer(tick);
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to GateIo.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("GateIo closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray btcUsd = new JSONArray();
            btcUsd.put(currency+"_USDT");
            btcUsd.put(5);
            btcUsd.put("0.01");

            JSONArray params = new JSONArray();
            params.put(btcUsd);

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("id", 12312);
            subscribeMessage.put("method", "depth.subscribe");
            subscribeMessage.put("params", params);

            client.send(subscribeMessage.toString());

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}