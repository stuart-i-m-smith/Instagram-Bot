package com.client.websocket;

import com.TickEventProcessor;
import com.model.CcyPair;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BybitClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public BybitClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://stream.bybit.com/spot/quote/ws/v2")) {

                private double lastSpotBid = 0;
                private double lastSpotAsk = 0;
                private double lastSpotBidSize = 0;
                private double lastSpotAskSize = 0;

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
                    double bidSize = bids.length() == 0 ? lastSpotBidSize : bids.getJSONArray(0).getDouble(1);
                    double askSize = asks.length() == 0 ? lastSpotAskSize : asks.getJSONArray(0).getDouble(1);


                    if (!Precision.equals(lastSpotBid, bid) ||
                        !Precision.equals(lastSpotAsk, ask) ||
                        !Precision.equals(lastSpotBidSize, bidSize) ||
                        !Precision.equals(lastSpotAskSize, askSize)) {

                        Tick tick = new Tick.Builder()
                                .exchange("bybit")
                                .timestamp(Instant.ofEpochMilli(data.getLong("t")))
                                .bid(bid)
                                .ask(ask)
                                .bidSize(bidSize)
                                .askSize(askSize)
                                .build();

                        lastSpotBid = bid;
                        lastSpotAsk = ask;
                        lastSpotBidSize = bidSize;
                        lastSpotAskSize = askSize;

                        tickEventProcessor.publishTicks(Set.of(tick));
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to Bybit.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Bybit closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject params = new JSONObject();
            params.put("symbol", ccyPair.getCcy1() + ccyPair.getCcy2());
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