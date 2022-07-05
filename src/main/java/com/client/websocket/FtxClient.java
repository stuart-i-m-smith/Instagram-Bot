package com.client.websocket;

import com.TickEventProcessor;
import com.model.Product;
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

public class FtxClient implements Client {

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public FtxClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ftx.com/ws/")) {

                private  double lastSpotBid = 0;
                private  double lastSpotAsk = 0;
                private  double lastFutureBid = 0;
                private  double lastFutureAsk = 0;

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);

                    if(!json.has("channel")){
                        return;
                    }

                    String channel = json.getString("channel");

                    if ("orderbook".equals(channel)) {

                        if(json.has("data")) {
                            JSONObject data = json.getJSONObject("data");
                            JSONArray bids = data.getJSONArray("bids");
                            JSONArray asks = data.getJSONArray("asks");

                            if(json.getString("market").contains("/USD")) {

                                double bid = bids.length() == 0 ? lastSpotBid : bids.getJSONArray(0).getDouble(0);
                                double ask = asks.length() == 0 ? lastSpotAsk : asks.getJSONArray(0).getDouble(0);

                                if (!Precision.equals(lastSpotBid, bid) ||
                                        !Precision.equals(lastSpotAsk, ask)) {

                                    Tick tick = new Tick.Builder()
                                            .exchange("ftx")
                                            .product(Product.Spot)
                                            .timestamp(Instant.ofEpochMilli(((Number) data.getDouble("time")).longValue() * 1000))
                                            .bid(bid)
                                            .ask(ask)
                                            .build();

                                    lastSpotBid = bid;
                                    lastSpotAsk = ask;

                                    tickEventProcessor.publishTick(tick);
                                }
                            }else{

                                double bid = bids.length() == 0 ? lastFutureBid : bids.getJSONArray(0).getDouble(0);
                                double ask = asks.length() == 0 ? lastFutureAsk : asks.getJSONArray(0).getDouble(0);

                                if (!Precision.equals(lastFutureBid, bid) ||
                                        !Precision.equals(lastFutureAsk, ask)) {

                                    Tick tick = new Tick.Builder()
                                            .exchange("ftx")
                                            .product(Product.Future)
                                            .timestamp(Instant.ofEpochMilli(((Number) data.getDouble("time")).longValue() * 1000))
                                            .bid(bid)
                                            .ask(ask)
                                            .build();

                                    lastFutureBid = bid;
                                    lastFutureAsk = ask;

                                    tickEventProcessor.publishTick(tick);
                                }
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
                    System.out.println("FTX closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("op", "subscribe");
            subscribeMessage.put("channel", "orderbook");

            subscribeMessage.put("market", currency+"/USD");
            client.send(subscribeMessage.toString());

            subscribeMessage.put("market", currency+"-PERP");
            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'op': 'ping'}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}