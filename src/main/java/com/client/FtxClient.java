package com.client;

import com.tick.TickManager;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FtxClient implements Client {

    private final TickManager tickManager;

    public FtxClient(TickManager manager){
        this.tickManager = manager;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ftx.com/ws/")) {

                private volatile double lastBid = 0;
                private volatile double lastAsk = 0;

                @Override
                public void onMessage(String message) {

                    JSONObject json = new JSONObject(message);

                    if(!json.has("channel")){
                        return;
                    }

                    String type = json.getString("channel");

                    if ("ticker".equals(type)) {

                        if(json.has("data")) {
                            JSONObject data = json.getJSONObject("data");
                            double bid = data.getDouble("bid");
                            double ask = data.getDouble("ask");

                            if(!Precision.equals(lastBid, bid) ||
                                !Precision.equals(lastAsk, ask)) {

                                Tick tick = new Tick.Builder()
                                        .exchange("ftx")
                                        .timestamp(Instant.ofEpochMilli(((Number) data.getDouble("time")).longValue() * 1000))
                                        .bid(bid)
                                        .ask(ask)
                                        .build();

                                lastBid = bid;
                                lastAsk = ask;

                                tickManager.offer(tick);
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to FTX.");
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

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("op", "subscribe");
            subscribeMessage.put("channel", "ticker");
            subscribeMessage.put("market", "ADA/USD");

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'op': 'ping'}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}