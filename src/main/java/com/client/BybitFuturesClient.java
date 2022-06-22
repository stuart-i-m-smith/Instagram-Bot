package com.client;

import com.model.Tick;
import com.tick.TickManager;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BybitFuturesClient implements Client {

    private final String currency;
    private final TickManager tickManager;

    public BybitFuturesClient(String currency, TickManager tickManager){
        this.currency = currency;
        this.tickManager = tickManager;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://stream.bybit.com/realtime_public")) {

                private volatile double lastSpotBid = 0;
                private volatile double lastSpotAsk = 0;

                @Override
                public void onMessage(String message) {
                    System.out.println(message);
                    JSONObject json = new JSONObject(message);

                    if(!json.has("data")){
                        return;
                    }

                    JSONObject data = json.getJSONObject("data");



                    JSONArray bids = data.getJSONArray("b");
                    JSONArray asks = data.getJSONArray("a");

                    double bid = bids.length() == 0 ? lastSpotBid : bids.getJSONArray(0).getDouble(0);
                    double ask = asks.length() == 0 ? lastSpotAsk : asks.getJSONArray(0).getDouble(0);

                    if (!Precision.equals(lastSpotBid, bid) ||
                        !Precision.equals(lastSpotAsk, ask)) {

                        Tick tick = new Tick.Builder()
                                .exchange("bybit")
                                .timestamp(Instant.ofEpochMilli(data.getLong("t")))
                                .bid(bid)
                                .ask(ask)
                                .build();

                        lastSpotBid = bid;
                        lastSpotAsk = ask;

                        tickManager.offer(tick);
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to Bybit.");
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

            JSONArray args = new JSONArray();
            args.put("orderBookL2_25."+currency+"USDT");

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("op", "subscribe");
            subscribeMessage.put("args", args);

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'op': 'ping'}"),
                    30, 30, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}