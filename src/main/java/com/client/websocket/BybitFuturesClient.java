package com.client.websocket;

import com.TickEventProcessor;
import com.model.Product;
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
    private final TickEventProcessor tickEventProcessor;

    public BybitFuturesClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
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
                    JSONObject json = new JSONObject(message);

                    if(!json.has("data")){
                        return;
                    }

                    JSONObject data = json.getJSONObject("data");

                    if(data.has("order_book")){

                        JSONArray orderBook = data.getJSONArray("order_book");

                        if(!orderBook.isEmpty()) {
                            JSONObject bidPrice = null;
                            JSONObject askPrice = null;

                            for (int i = 0; i < orderBook.length(); i++) {

                                JSONObject price = orderBook.getJSONObject(i);
                                if ("Buy".equals(price.getString("side"))) {
                                    bidPrice = price;
                                }

                                if ("Sell".equals(price.getString("side"))) {
                                    askPrice = price;
                                }

                                if(askPrice != null){
                                    break;
                                }
                            }

                            double bid = bidPrice == null ? lastSpotBid : bidPrice.getDouble("price");
                            double ask = askPrice == null ? lastSpotAsk : askPrice.getDouble("price");
                            lastBidId = bidPrice == null ? lastBidId : bidPrice.getString("id");
                            lastAskId = askPrice == null ? lastAskId : askPrice.getString("id");

                            if (!Precision.equals(lastSpotBid, bid) ||
                                    !Precision.equals(lastSpotAsk, ask)) {

                                Tick tick = new Tick.Builder()
                                        .exchange("bybit")
                                        .timestamp(Instant.ofEpochMilli(json.getLong("timestamp_e6") / 1000))
                                        .bid(bid)
                                        .ask(ask)
                                        .build();

                                lastSpotBid = bid;
                                lastSpotAsk = ask;

                                tickEventProcessor.publishTick(tick);
                            }
                        }
                    }

                    if(data.has("delete")){
                        JSONArray delete = data.getJSONArray("delete");

                        for (int i = 0; i < delete.length(); i++) {

                            JSONObject price = delete.getJSONObject(i);
                            if ("Buy".equals(price.getString("side")) && price.getString("id").equals(lastBidId)) {
                                lastBidId = null;
                                lastSpotBid = 0;
                            }

                            if ("Sell".equals(price.getString("side")) && price.getString("id").equals(lastAskId)) {
                                lastAskId = null;
                                lastSpotAsk = Double.MAX_VALUE;
                            }
                        }
                    }

                    if(data.has("update")){
                        JSONArray update = data.getJSONArray("update");

                        JSONObject bidPrice = null;
                        JSONObject askPrice = null;

                        for (int i = 0; i < update.length(); i++) {

                            JSONObject price = update.getJSONObject(i);
                            if ("Buy".equals(price.getString("side")) && price.getDouble("price") > lastSpotBid) {
                                bidPrice = price;
                            }

                            if ("Sell".equals(price.getString("side")) && price.getDouble("price") < lastSpotAsk) {
                                if(lastSpotAsk == Double.MAX_VALUE){
                                    lastSpotAsk = price.getDouble("price");
                                }

                                askPrice = price;
                            }
                        }

                        double bid = bidPrice == null ? lastSpotBid : bidPrice.getDouble("price");
                        double ask = askPrice == null ? lastSpotAsk : askPrice.getDouble("price");
                        lastBidId = bidPrice == null ? lastBidId : bidPrice.getString("id");
                        lastAskId = askPrice == null ? lastAskId : askPrice.getString("id");

                        if (!Precision.equals(lastSpotBid, bid) ||
                            !Precision.equals(lastSpotAsk, ask)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("bybit")
                                    .product(Product.Future)
                                    .timestamp(Instant.ofEpochMilli(json.getLong("timestamp_e6") / 1000))
                                    .bid(bid)
                                    .ask(ask)
                                    .build();

                            lastSpotBid = bid;
                            lastSpotAsk = ask;

                            tickEventProcessor.publishTick(tick);
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("Connected to Bybit Futures.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Bybit Futures closed connection");
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

            //'{"op": "subscribe", "args": ["instrument_info.100ms.BTCUSDT"]}'

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'op': 'ping'}"),
                    30, 30, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}