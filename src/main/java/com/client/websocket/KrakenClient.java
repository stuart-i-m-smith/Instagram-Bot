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

public class KrakenClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public KrakenClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.kraken.com")) {

                private  double lastBid = 0;
                private  double lastAsk = 0;
                private  double lastBidSize = 0;
                private  double lastAskSize = 0;

                @Override
                public void onMessage(String message) {
                    if(!message.contains("\"event\":")){

                        JSONObject json = (JSONObject) new JSONObject("{\"pair\": "+message+"}")
                                .getJSONArray("pair")
                                .get(1);

                        JSONArray bids = json.getJSONArray("b");
                        JSONArray offers = json.getJSONArray("a");
                        double bid = bids.getDouble(0);
                        double ask = offers.getDouble(0);
                        double bidSize = bids.getDouble(1);
                        double askSize = offers.getDouble(1);

                        if(!Precision.equals(lastBid, bid) ||
                            !Precision.equals(lastAsk, ask) ||
                            !Precision.equals(lastBidSize, bidSize) ||
                            !Precision.equals(lastAskSize, askSize)) {

                            Tick tick = new Tick.Builder()
                                    .exchange("kraken")
                                    .timestamp(Instant.now())
                                    .bid(bid)
                                    .ask(ask)
                                    .bidSize(bidSize)
                                    .askSize(askSize)
                                    .build();

                            lastBid = bid;
                            lastAsk = ask;
                            lastBidSize = bidSize;
                            lastAskSize = askSize;

                            tickEventProcessor.publishTicks(Set.of(tick));
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to Kraken.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("Kraken closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray pairs = new JSONArray();
            pairs.put(ccyPair.getCcy1() +"/"+ ccyPair.getCcy2().replace("USDT", "USD"));

            JSONObject subscription = new JSONObject();
            subscription.put("name", "ticker");

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("event", "subscribe");
            subscribeMessage.put("pair", pairs);
            subscribeMessage.put("subscription", subscription);

            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{\"event\": \"ping\"}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}