package com.client.websocket;

import com.TickEventProcessor;
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

public class BybitClient implements Client {

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public BybitClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://stream.bybit.com/spot/quote/ws/v2")) {

                private volatile double lastSpotBid = 0;
                private volatile double lastSpotAsk = 0;

                @Override
                public void onMessage(String message) {
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

                        tickEventProcessor.publishTick(tick);
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to Bybit.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Bybit closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject params = new JSONObject();
            params.put("symbol", currency+"USDT");
            params.put("binary", false);

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("topic", "depth");
            subscribeMessage.put("event", "sub");
            subscribeMessage.put("params", params);

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'ping': "+System.currentTimeMillis()+"}"),
                    30, 30, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}