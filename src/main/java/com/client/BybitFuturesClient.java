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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
                private volatile String lastBidId = null;
                private volatile String lastAskId = null;

                @Override
                public void onMessage(String message) {
                    System.out.println(message);
                    JSONObject json = new JSONObject(message);

                    if(!json.has("data")){
                        return;
                    }

                    JSONObject data = json.getJSONObject("data");
                    JSONArray orderBook = data.getJSONArray("order_book");

                    if(!orderBook.isEmpty()) {
                        JSONObject bidPrice = null;
                        JSONObject askPrice = null;

                        for(int i=0;i<orderBook.length();i++){

                            JSONObject price = orderBook.getJSONObject(i);
                            if("Buy".equals(price.getString("side")) && askPrice == null){
                                askPrice = price;
                            }

                            if("Sell".equals(price.getString("side"))){
                                bidPrice = price;
                            }
                        }

                        double bid = bidPrice == null ? lastSpotBid : bidPrice.getDouble("price");
                        double ask = askPrice == null ? lastSpotAsk : askPrice.getDouble("price");

                        if (!Precision.equals(lastSpotBid, bid) ||
                            !Precision.equals(lastSpotAsk, ask)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("bybit")
                                    .timestamp(Instant.ofEpochMilli(json.getLong("timestamp_e6")/1000))
                                    .bid(bid)
                                    .ask(ask)
                                    .build();

                            lastSpotBid = bid;
                            lastSpotAsk = ask;

                            tickManager.offer(tick);
                        }
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