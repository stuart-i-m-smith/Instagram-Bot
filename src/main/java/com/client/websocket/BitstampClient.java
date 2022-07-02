package com.client.websocket;

import com.TickEventProcessor;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BitstampClient implements Client {

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public BitstampClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.bitstamp.net")) {

                private volatile double lastBid = 0;
                private volatile double lastAsk = 0;

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);

                    if(json.has("data")){
                        JSONObject data = json.getJSONObject("data");

                        if(data.has("bids") || data.has("asks")){
                            Tick.Builder tickBuilder = new Tick.Builder()
                                    .exchange("bitstamp")
                                    .timestamp(Instant.ofEpochMilli(data.getLong("timestamp")));

                            if(data.has("bids")){
                                tickBuilder.bid(data.getJSONArray("bids")
                                    .getJSONArray(0)
                                    .getDouble(0));
                            }

                            if(data.has("asks")){
                                tickBuilder.ask(data.getJSONArray("asks")
                                    .getJSONArray(0)
                                    .getDouble(0));
                            }

                            Tick tick = tickBuilder.build();

                            if(!Precision.equals(lastBid, tick.getBid()) ||
                                !Precision.equals(lastAsk, tick.getAsk())) {

                                lastBid = tick.getBid();
                                lastAsk = tick.getAsk();

                                tickEventProcessor.publishTick(tick);
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to Bistamp.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Bitstamp closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject data = new JSONObject();
            data.put("channel", "order_book_"+currency+"usd");

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("event", "bts:subscribe");
            subscribeMessage.put("data", data);

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{\"event\": \"bts:heartbeat\"}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}