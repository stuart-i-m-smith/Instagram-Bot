package com.client.websocket;

import com.TickEventProcessor;
import com.model.CcyPair;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BitstampClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public BitstampClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.bitstamp.net")) {

                private double lastBid = 0;
                private double lastAsk = 0;
                private double lastBidSize = 0;
                private double lastAskSize = 0;

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

                                tickBuilder.bidSize(data.getJSONArray("bids")
                                        .getJSONArray(0)
                                        .getDouble(1));
                            }

                            if(data.has("asks")){
                                tickBuilder.ask(data.getJSONArray("asks")
                                    .getJSONArray(0)
                                    .getDouble(0));

                                tickBuilder.askSize(data.getJSONArray("asks")
                                        .getJSONArray(0)
                                        .getDouble(1));
                            }

                            Tick tick = tickBuilder.build();

                            if(!Precision.equals(lastBid, tick.getBid()) ||
                                !Precision.equals(lastAsk, tick.getAsk()) ||
                                !Precision.equals(lastBidSize, tick.getBidSize()) ||
                                !Precision.equals(lastAskSize, tick.getAskSize())) {

                                lastBid = tick.getBid();
                                lastAsk = tick.getAsk();
                                lastBidSize = tick.getBidSize();
                                lastAskSize = tick.getAskSize();

                                tickEventProcessor.publishTicks(Set.of(tick));
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to Bistamp.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Bitstamp closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject data = new JSONObject();
            data.put("channel", "order_book_"+ccyPair.getCcy1() + ccyPair.getCcy2().replace("USDT", "USD"));

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