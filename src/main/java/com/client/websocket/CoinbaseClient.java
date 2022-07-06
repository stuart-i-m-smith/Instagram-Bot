package com.client.websocket;

import com.TickEventProcessor;
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
import java.util.concurrent.TimeUnit;

public class CoinbaseClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public CoinbaseClient(String currency, TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {

        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws-feed.exchange.coinbase.com")) {

                private  double lastBid = 0;
                private  double lastBidSize = 0;
                private  double lastAsk = 0;
                private  double lastAskSize = 0;

                @Override
                public void onMessage(String message) {

                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    if (("snapshot").equals(type)) {
                        JSONArray bids = json.getJSONArray("bids");
                        JSONArray asks = json.getJSONArray("asks");

                        double bid = bids.length() > 0 ? bids.getJSONArray(0).getDouble(0) : lastBid;
                        double ask = asks.length() > 0 ? asks.getJSONArray(0).getDouble(0) : lastAsk;
                        double bidSize = bids.length() > 0 ? bids.getJSONArray(1).getDouble(0) : lastBidSize;
                        double askSize = asks.length() > 0 ? asks.getJSONArray(1).getDouble(0) : lastAskSize;

                        if(!Precision.equals(lastBid, bid) ||
                            !Precision.equals(lastAsk, ask) ||
                            !Precision.equals(lastBidSize, bidSize) ||
                            !Precision.equals(lastAskSize, askSize)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("coinbase")
                                    .timestamp(Instant.parse(json.getString("time")))
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

                    if (("l2update").equals(type)) {
                        JSONArray changes = json.getJSONArray("changes");

                        for(int i=0;i<changes.length();i++){

                            JSONArray change = changes.getJSONArray(i);

                            JSONArray bids = json.getJSONArray("changes");
                            JSONArray asks = json.getJSONArray("asks");

                            double bid = bids.length() > 0 ? bids.getJSONArray(0).getDouble(0) : lastBid;
                            double ask = asks.length() > 0 ? asks.getJSONArray(0).getDouble(0) : lastAsk;
                            double bidSize = bids.length() > 0 ? bids.getJSONArray(1).getDouble(0) : lastBidSize;
                            double askSize = asks.length() > 0 ? asks.getJSONArray(1).getDouble(0) : lastAskSize;

                            if (!Precision.equals(lastBid, bid) ||
                                    !Precision.equals(lastAsk, ask) ||
                                    !Precision.equals(lastBidSize, bidSize) ||
                                    !Precision.equals(lastAskSize, askSize)) {

                                Tick tick = new Tick.Builder()
                                        .exchange("coinbase")
                                        .timestamp(Instant.parse(json.getString("time")))
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
                }

                @Override
                public void onOpen(ServerHandshake handshake) {
                    LOGGER.info("Connected to Coinbase.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Coinbase closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray products = new JSONArray();
            products.put(currency+"-USD");

            JSONArray channels = new JSONArray();
            channels.put("level2");

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("type", "subscribe");
            subscribeMessage.put("product_ids", products);
            subscribeMessage.put("channels", channels);

            client.send(subscribeMessage.toString());

        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }
}
