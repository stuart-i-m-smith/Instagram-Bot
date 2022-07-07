package com.client.websocket;

import com.TickEventProcessor;
import com.model.Bucket;
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
import java.util.Comparator;
import java.util.TreeSet;
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

                private final TreeSet<Bucket> bidBuckets = new TreeSet<>(Comparator.comparing(b -> ((Bucket)b).getPrice()).reversed());
                private final TreeSet<Bucket> askBuckets = new TreeSet<>(Comparator.comparing(Bucket::getPrice));

                @Override
                public void onMessage(String message) {

                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    if (("snapshot").equals(type)) {
                        JSONArray bids = json.getJSONArray("bids");
                        JSONArray asks = json.getJSONArray("asks");

                        for(int i=0;i<bids.length();i++){
                            JSONArray bidBucketJson = bids.getJSONArray(i);

                            bidBuckets.add(new Bucket(
                                bidBucketJson.getDouble(0),
                                bidBucketJson.getDouble(1)));
                        }

                        for(int i=0;i<asks.length();i++){
                            JSONArray askBucketJson = asks.getJSONArray(i);

                            askBuckets.add(new Bucket(
                                askBucketJson.getDouble(0),
                                askBucketJson.getDouble(1)));
                        }

                        if(bidBuckets.size() > 0 || askBuckets.size() > 0) {

                            Tick.Builder tick = new Tick.Builder()
                                .exchange("coinbase")
                                .timestamp(Instant.now());

                            if(bidBuckets.size() > 0){
                                tick.bid(bidBuckets.first().getPrice())
                                    .bidSize(bidBuckets.first().getSize());
                            }

                            if(askBuckets.size() > 0) {
                                tick.ask(askBuckets.first().getPrice())
                                    .askSize(askBuckets.first().getSize());
                            }
                            tickEventProcessor.publishTick(tick.build());
                        }
                    }

                    if (("l2update").equals(type)) {
                        JSONArray changes = json.getJSONArray("changes");

                        Bucket lastBidBucket = bidBuckets.size() > 0 ? bidBuckets.first() : new Bucket(0);
                        Bucket lastAskBucket = askBuckets.size() > 0 ? askBuckets.first() : new Bucket(0);

                        for(int i=0;i<changes.length();i++){

                            JSONArray change = changes.getJSONArray(i);

                            if("buy".equals(change.getString(0))){
                                double price = change.getDouble(1);
                                double size = change.getDouble(2);

                                if(Precision.equals(0, size)){
                                    bidBuckets.remove(new Bucket(price));
                                }else {
                                    bidBuckets.add(new Bucket(price, size));
                                }
                            }

                            if("sell".equals(change.getString(0))){

                                double price = change.getDouble(1);
                                double size = change.getDouble(2);

                                if(Precision.equals(0, size)){
                                    askBuckets.remove(new Bucket(price));
                                }else {
                                    askBuckets.add(new Bucket(price, size));
                                }
                            }

                            Bucket latestBidBucket = bidBuckets.size() > 0 ? bidBuckets.first() : null;
                            Bucket latestAskBucket = askBuckets.size() > 0 ? askBuckets.first() : null;

                            Tick.Builder tick = new Tick.Builder()
                                    .exchange("coinbase")
                                    .timestamp(Instant.parse(json.getString("time")));

                            if(latestBidBucket != null && (
                                !latestBidBucket.equals(lastBidBucket) ||
                                !Precision.equals(latestBidBucket.getSize(), lastBidBucket.getSize()))) {

                                tick.bid(latestBidBucket.getPrice())
                                    .bidSize(latestBidBucket.getSize());
                            }else{
                                tick.bid(lastBidBucket.getPrice())
                                    .bidSize(lastBidBucket.getSize());
                            }

                            if(latestAskBucket != null && (
                                !latestAskBucket.equals(lastAskBucket) ||
                                !Precision.equals(latestAskBucket.getSize(), lastAskBucket.getSize()))) {

                                tick.ask(latestAskBucket.getPrice())
                                    .askSize(latestAskBucket.getSize());
                            }else{
                                tick.ask(lastAskBucket.getPrice())
                                    .askSize(lastAskBucket.getSize());
                            }

                            tickEventProcessor.publishTick(tick.build());
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake handshake) {
                    LOGGER.info("Connected to Coinbase.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Coinbase closed connection <{}> <{}>", code, reason);
                }

                @Override
                public void onError(Exception e) {
                    LOGGER.error("Error with Coinbase connection.", e);
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