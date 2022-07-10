package com.client.websocket;

import com.TickEventProcessor;
import com.model.CcyPair;
import com.model.Product;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BybitFuturesClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public BybitFuturesClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://stream.bybit.com/realtime_public")) {

                private double lastBid = 0;
                private double lastAsk = 0;
                private double lastBidSize = 0;
                private double lastAskSize = 0;
                private String lastBidId = null;
                private String lastAskId = null;

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

                            double bid = bidPrice == null ? lastBid : bidPrice.getDouble("price");
                            double ask = askPrice == null ? lastAsk : askPrice.getDouble("price");
                            double bidSize = bidPrice == null ? lastBidSize : bidPrice.getDouble("size");
                            double askSize = askPrice == null ? lastAskSize : askPrice.getDouble("size");
                            lastBidId = bidPrice == null ? lastBidId : bidPrice.getString("id");
                            lastAskId = askPrice == null ? lastAskId : askPrice.getString("id");

                            if (!Precision.equals(lastBid, bid) ||
                                !Precision.equals(lastAsk, ask) ||
                                !Precision.equals(lastBidSize, bidSize) ||
                                !Precision.equals(lastAskSize, askSize)) {

                                Tick tick = new Tick.Builder()
                                        .exchange("bybit")
                                        .timestamp(Instant.ofEpochMilli(json.getLong("timestamp_e6") / 1000))
                                        .bid(bid)
                                        .ask(ask)
                                        .bidSize(bidSize)
                                        .askSize(askSize)
                                        .build();

                                lastBid = bid;
                                lastAsk = ask;
                                lastBidSize = bidSize;
                                lastAskSize = askSize;

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
                                lastBid = 0;
                                lastBidSize = 0;
                            }

                            if ("Sell".equals(price.getString("side")) && price.getString("id").equals(lastAskId)) {
                                lastAskId = null;
                                lastAsk = Double.MAX_VALUE;
                                lastAskSize = 0;
                            }
                        }
                    }

                    if(data.has("update")){
                        JSONArray update = data.getJSONArray("update");

                        JSONObject bidPrice = null;
                        JSONObject askPrice = null;

                        for (int i = 0; i < update.length(); i++) {

                            JSONObject price = update.getJSONObject(i);
                            if ("Buy".equals(price.getString("side")) && price.getDouble("price") > lastBid) {
                                bidPrice = price;
                            }

                            if ("Sell".equals(price.getString("side")) && price.getDouble("price") < lastAsk) {
                                if(lastAsk == Double.MAX_VALUE){
                                    lastAsk = price.getDouble("price");
                                }

                                askPrice = price;
                            }
                        }

                        double bid = bidPrice == null ? lastBid : bidPrice.getDouble("price");
                        double ask = askPrice == null ? lastAsk : askPrice.getDouble("price");
                        double bidSize = bidPrice == null ? lastBidSize : bidPrice.getDouble("size");
                        double askSize = askPrice == null ? lastAskSize : askPrice.getDouble("size");
                        lastBidId = bidPrice == null ? lastBidId : bidPrice.getString("id");
                        lastAskId = askPrice == null ? lastAskId : askPrice.getString("id");

                        if (!Precision.equals(lastBid, bid) ||
                            !Precision.equals(lastAsk, ask) ||
                            !Precision.equals(lastBidSize, bidSize) ||
                            !Precision.equals(lastAskSize, askSize)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("bybit")
                                    .product(Product.Future)
                                    .timestamp(Instant.ofEpochMilli(json.getLong("timestamp_e6") / 1000))
                                    .bid(bid)
                                    .ask(ask)
                                    .bidSize(bidSize)
                                    .askSize(askSize)
                                    .build();

                            lastBid = bid;
                            lastAsk = ask;
                            lastBidSize = bidSize;
                            lastAskSize = askSize;

                            tickEventProcessor.publishTick(tick);
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to Bybit Futures.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Bybit Futures closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray args = new JSONArray();
            args.put("orderBookL2_25."+ccyPair.getCcy1() + ccyPair.getCcy2());

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